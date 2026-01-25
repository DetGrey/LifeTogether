package com.example.lifetogether.ui.model

sealed interface MenuAction {
    val label: String // Every action must have a label

    enum class AlbumActions(override val label: String) : MenuAction {
        RENAME("Rename album"),
        DELETE("Delete album")
    }

    enum class SelectionActions(override val label: String) : MenuAction {
        MOVE("Move to album"),
        DOWNLOAD("Download selected"),
        DELETE("Delete selected")
    }

    enum class GalleryMediaActions(override val label: String) : MenuAction {
        DOWNLOAD("Download"),
        DELETE("Delete"),
    }
}