package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class TravellerFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "TravellerFirestoreDS"
    }

    fun travellerPinsSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.FAMILIES_TABLE)
            .document(familyId)
            .collection(Constants.TRAVELLER_PINS_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = mapFirestoreDocuments(
                    tag = TAG,
                    collectionName = Constants.TRAVELLER_PINS_TABLE,
                    entityName = "TravellerPin",
                    documents = snapshot.documents,
                ) { doc ->
                    doc.toObject(TravellerPinDto::class.java)?.toDomain(doc.id, familyId)
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun savePin(pin: TravellerPin): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.FAMILIES_TABLE)
                .document(pin.familyId)
                .collection(Constants.TRAVELLER_PINS_TABLE)
                .document(pin.id)
                .set(pin.toDto().toFirestoreMap())
                .await()
        }
    }

    suspend fun deletePin(familyId: String, pinId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.FAMILIES_TABLE)
                .document(familyId)
                .collection(Constants.TRAVELLER_PINS_TABLE)
                .document(pinId)
                .delete()
                .await()
        }
    }
}

private data class TravellerPinDto(
    val lastUpdated: Date? = null,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String? = null,
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val albumId: String? = null,
) {
    fun toDomain(documentId: String, familyId: String): TravellerPin? {
        val cityValue = city?.takeIf { it.isNotBlank() } ?: return null
        val countryValue = country?.takeIf { it.isNotBlank() } ?: return null
        val latValue = latitude ?: return null
        val lngValue = longitude ?: return null
        val typeValue = type?.let { t -> PinType.entries.firstOrNull { it.name == t } } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        return TravellerPin(
            id = documentId,
            familyId = familyId,
            lastUpdated = lastUpdatedValue,
            city = cityValue,
            country = countryValue,
            latitude = latValue,
            longitude = lngValue,
            type = typeValue,
            dateFrom = dateFrom,
            dateTo = dateTo,
            albumId = albumId,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "lastUpdated" to lastUpdated,
        "city" to city,
        "country" to country,
        "latitude" to latitude,
        "longitude" to longitude,
        "type" to type,
        "dateFrom" to dateFrom,
        "dateTo" to dateTo,
        "albumId" to albumId,
    )
}

private fun TravellerPin.toDto() = TravellerPinDto(
    lastUpdated = lastUpdated,
    city = city,
    country = country,
    latitude = latitude,
    longitude = longitude,
    type = type.name,
    dateFrom = dateFrom,
    dateTo = dateTo,
    albumId = albumId,
)
