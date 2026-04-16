package com.example.lifetogether.data.local.source.query

import com.example.lifetogether.util.Constants

enum class ListQueryType(
    val tableName: String,
) {
    Grocery(Constants.GROCERY_TABLE),
    Recipes(Constants.RECIPES_TABLE),
    Albums(Constants.ALBUMS_TABLE),
    GalleryMedia(Constants.GALLERY_MEDIA_TABLE),
    TipTracker(Constants.TIP_TRACKER_TABLE),
    Guides(Constants.GUIDES_TABLE),
    UserLists(Constants.USER_LISTS_TABLE),
    RoutineListEntries(Constants.ROUTINE_LIST_ENTRIES_TABLE),
}
