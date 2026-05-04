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
    WISH_LIST_ENTRIES,
    NOTE_ENTRIES,
    CHECKLIST_ENTRIES,
    MEAL_PLAN_ENTRIES,
}

data class SyncContext(
    val uid: String? = null,
    val familyId: String? = null,
)
