package com.example.lifetogether.data.logic

import android.database.sqlite.SQLiteException
import com.example.lifetogether.domain.result.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.serialization.SerializationException

fun Throwable.toAppError(): AppError {
    val fallbackMessage = message ?: "Unknown error"
    return when (this) {
        is FirebaseNetworkException, is IOException -> AppError.Network(fallbackMessage)
        is FirebaseAuthInvalidUserException, is FirebaseAuthException -> AppError.Authentication(fallbackMessage)
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied(fallbackMessage)
            FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound(fallbackMessage)
            FirebaseFirestoreException.Code.ALREADY_EXISTS,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            -> AppError.Conflict(fallbackMessage)
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> AppError.Validation(fallbackMessage)
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            -> AppError.Network(fallbackMessage)
            else -> AppError.Storage(fallbackMessage)
        }

        is SerializationException -> AppError.Serialization(fallbackMessage)
        is IllegalArgumentException -> AppError.Validation(fallbackMessage)
        is SQLiteException -> AppError.Storage(fallbackMessage)
        else -> AppError.Unknown(fallbackMessage)
    }
}
