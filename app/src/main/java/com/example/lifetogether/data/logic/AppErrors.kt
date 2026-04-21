package com.example.lifetogether.data.logic

import android.util.Log
import android.database.sqlite.SQLiteException
import com.example.lifetogether.domain.result.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.serialization.SerializationException

object AppErrors {
    private const val TAG = "AppErrors"

    fun fromThrowable(
        throwable: Throwable,
        source: String? = null,
        messageOverride: String? = null,
    ): AppError {
        val fallbackMessage = messageOverride ?: throwable.message ?: "Unknown error"
        return when (throwable) {
            is FirebaseNetworkException, is IOException -> network(fallbackMessage, source, throwable)
            is FirebaseAuthInvalidUserException, is FirebaseAuthException -> authentication(fallbackMessage, source, throwable)
            is FirebaseFirestoreException -> when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> permissionDenied(fallbackMessage, source, throwable)
                FirebaseFirestoreException.Code.NOT_FOUND -> notFound(fallbackMessage, source, throwable)
                FirebaseFirestoreException.Code.ALREADY_EXISTS,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.FAILED_PRECONDITION,
                -> conflict(fallbackMessage, source, throwable)

                FirebaseFirestoreException.Code.INVALID_ARGUMENT -> validation(fallbackMessage, source, throwable)
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
                -> network(fallbackMessage, source, throwable)

                else -> storage(fallbackMessage, source, throwable)
            }

            is SerializationException -> serialization(fallbackMessage, source, throwable)
            is IllegalArgumentException -> validation(fallbackMessage, source, throwable)
            is SQLiteException -> storage(fallbackMessage, source, throwable)
            else -> unknown(fallbackMessage, source, throwable)
        }
    }

    fun network(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Network", message, source, cause) { AppError.Network(message) }

    fun authentication(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Authentication", message, source, cause) { AppError.Authentication(message) }

    fun permissionDenied(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("PermissionDenied", message, source, cause) { AppError.PermissionDenied(message) }

    fun notFound(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("NotFound", message, source, cause) { AppError.NotFound(message) }

    fun conflict(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Conflict", message, source, cause) { AppError.Conflict(message) }

    fun validation(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Validation", message, source, cause) { AppError.Validation(message) }

    fun storage(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Storage", message, source, cause) { AppError.Storage(message) }

    fun serialization(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Serialization", message, source, cause) { AppError.Serialization(message) }

    fun unknown(message: String, source: String? = null, cause: Throwable? = null): AppError =
        create("Unknown", message, source, cause) { AppError.Unknown(message) }

    private inline fun create(
        type: String,
        message: String,
        source: String?,
        cause: Throwable?,
        factory: () -> AppError,
    ): AppError {
        Log.d(
            TAG,
            "errorType=$type source=${source ?: "unspecified"} message=$message " +
                "causeType=${cause?.javaClass?.simpleName ?: "none"} " +
                "causeMessage=${cause?.message ?: "none"}",
        )
        return factory()
    }
}
