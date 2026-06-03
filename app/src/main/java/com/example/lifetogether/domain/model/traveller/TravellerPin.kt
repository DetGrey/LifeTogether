package com.example.lifetogether.domain.model.traveller

import java.util.Date

data class TravellerPin(
    val id: String,
    val familyId: String,
    val lastUpdated: Date = Date(),
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val type: PinType,
    // Required for VISITED/LIVED, optional for BUCKET_LIST
    val dateFrom: Date?,
    // null only valid when type == LIVED (still living there).
    // For VISITED single-day trips: dateTo == dateFrom.
    val dateTo: Date?,
    val albumId: String?,
)
