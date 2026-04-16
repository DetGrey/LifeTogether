package com.example.lifetogether.data.local.source.query

object ListQueryTypeMapper {
    // TODO(v2-phase2-cleanup): remove temporary String -> ListQueryType mapping once callers pass typed query keys end-to-end.
    fun fromTableNameOrNull(tableName: String): ListQueryType? = when (tableName) {
        ListQueryType.Grocery.tableName -> ListQueryType.Grocery
        ListQueryType.Recipes.tableName -> ListQueryType.Recipes
        ListQueryType.Albums.tableName -> ListQueryType.Albums
        ListQueryType.GalleryMedia.tableName -> ListQueryType.GalleryMedia
        ListQueryType.TipTracker.tableName -> ListQueryType.TipTracker
        ListQueryType.Guides.tableName -> ListQueryType.Guides
        ListQueryType.UserLists.tableName -> ListQueryType.UserLists
        ListQueryType.RoutineListEntries.tableName -> ListQueryType.RoutineListEntries
        else -> null
    }
}
