package com.example.lifetogether.domain.observer

enum class ObserverKey {
    USER,
    FAMILY,
    GROCERY_LIST,
    GROCERY_CATEGORIES,
    GROCERY_SUGGESTIONS,
    RECIPES,
    GUIDES,
    TIP_TRACKER,
    GALLERY_ALBUMS,
    GALLERY_MEDIA,
    USER_LISTS,
    ROUTINE_LIST_ENTRIES,
}

enum class ObserverSyncState {
    IDLE,
    UPDATING,
    READY,
    FAILED,
}

data class ObserverContext(
    val uid: String? = null,
    val familyId: String? = null,
)
