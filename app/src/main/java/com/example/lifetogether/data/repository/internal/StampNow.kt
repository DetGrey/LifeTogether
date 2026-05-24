package com.example.lifetogether.data.repository.internal

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.model.recipe.Recipe
import java.util.Date

fun Recipe.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun MealPlan.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun UserList.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun RoutineListEntry.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun WishListEntry.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun NoteEntry.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun ChecklistEntry.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun Album.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun GalleryMedia.stampNow(now: Date = Date()): GalleryMedia = when (this) {
    is GalleryImage -> copy(lastUpdated = now)
    is GalleryVideo -> copy(lastUpdated = now)
}
fun List<GalleryMedia>.stampNow(now: Date = Date()): List<GalleryMedia> = map { it.stampNow(now) }
fun Guide.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun GroceryItem.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun TipItem.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun UserInformation.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun Category.stampNow(now: Date = Date()) = copy(lastUpdated = now)
fun GrocerySuggestion.stampNow(now: Date = Date()) = copy(lastUpdated = now)
