package com.example.lifetogether.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot

internal const val FIRESTORE_SKIP_INVALID_DOC_PREFIX = "FIRESTORE_SKIP_INVALID_DOC"

internal inline fun <T> mapFirestoreDocuments(
    tag: String,
    collectionName: String,
    entityName: String,
    documents: Iterable<DocumentSnapshot>,
    crossinline mapper: (DocumentSnapshot) -> T?,
): List<T> {
    return documents.mapNotNull { document ->
        mapFirestoreDocument(
            tag = tag,
            collectionName = collectionName,
            entityName = entityName,
            document = document,
            mapper = mapper,
        )
    }
}

internal inline fun <T> mapFirestoreDocument(
    tag: String,
    collectionName: String,
    entityName: String,
    document: DocumentSnapshot,
    crossinline mapper: (DocumentSnapshot) -> T?,
): T? {
    return try {
        val mapped = mapper(document)
        if (mapped == null) {
            Log.w(
                tag,
                "$FIRESTORE_SKIP_INVALID_DOC_PREFIX collection=$collectionName entity=$entityName docId=${document.id} reason=missing required fields",
            )
        }
        mapped
    } catch (throwable: Throwable) {
        Log.w(
            tag,
            "$FIRESTORE_SKIP_INVALID_DOC_PREFIX collection=$collectionName entity=$entityName docId=${document.id} reason=exception",
            throwable,
        )
        null
    }
}
