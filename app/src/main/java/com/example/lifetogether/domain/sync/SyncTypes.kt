package com.example.lifetogether.domain.sync

enum class SyncKey {
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

enum class SyncState {
    IDLE,
    UPDATING,
    READY,
    FAILED,
}

data class SyncContext(
    val uid: String? = null,
    val familyId: String? = null,
)
