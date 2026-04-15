package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.dao.GuideProgressDao
import com.example.lifetogether.data.local.dao.GuidesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.TipTrackerDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.model.GuideProgressEntity
import com.example.lifetogether.data.model.GuideEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.ListCountEntity
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.model.UserEntity

@Database(
    entities = [
        GroceryListEntity::class,
        GrocerySuggestionEntity::class,
        ListCountEntity::class,
        CategoryEntity::class,
        UserEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class,
        RecipeEntity::class,
        AlbumEntity::class,
        GalleryMediaEntity::class,
        TipEntity::class,
        GuideEntity::class,
        GuideProgressEntity::class,
        UserListEntity::class,
        RoutineListEntryEntity::class,
    ],
    version = 26,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    abstract fun grocerySuggestionsDao(): GrocerySuggestionsDao
    abstract fun userInformationDao(): UserInformationDao
    abstract fun familyInformationDao(): FamilyInformationDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun recipesDao(): RecipesDao
    abstract fun albumsDao(): AlbumsDao
    abstract fun galleryMediaDao(): GalleryMediaDao
    abstract fun tipTrackerDao(): TipTrackerDao
    abstract fun guidesDao(): GuidesDao
    abstract fun guideProgressDao(): GuideProgressDao
    abstract fun userListsDao(): UserListsDao
    abstract fun routineListsDao(): RoutineListsDao

    companion object {
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite doesn't support DROP COLUMN before 3.35, so recreate without image_url
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_lists_new` (
                        `id` TEXT NOT NULL,
                        `family_id` TEXT NOT NULL,
                        `item_name` TEXT NOT NULL,
                        `last_updated` INTEGER NOT NULL,
                        `date_created` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `visibility` TEXT NOT NULL,
                        `owner_uid` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `user_lists_new` SELECT `id`, `family_id`, `item_name`, `last_updated`, `date_created`, `type`, `visibility`, `owner_uid` FROM `user_lists`"
                )
                db.execSQL("DROP TABLE `user_lists`")
                db.execSQL("ALTER TABLE `user_lists_new` RENAME TO `user_lists`")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `list_entries_routine` ADD COLUMN `image_data` BLOB")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_lists` (
                        `id` TEXT NOT NULL,
                        `family_id` TEXT NOT NULL,
                        `item_name` TEXT NOT NULL,
                        `last_updated` INTEGER,
                        `date_created` INTEGER,
                        `type` TEXT NOT NULL,
                        `visibility` TEXT NOT NULL,
                        `owner_uid` TEXT NOT NULL,
                        `image_url` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `list_entries_routine` (
                        `id` TEXT NOT NULL,
                        `family_id` TEXT NOT NULL,
                        `list_id` TEXT NOT NULL,
                        `item_name` TEXT NOT NULL,
                        `last_updated` INTEGER,
                        `date_created` INTEGER,
                        `next_date` INTEGER,
                        `last_completed_at` INTEGER,
                        `completion_count` INTEGER NOT NULL,
                        `recurrence_unit` TEXT NOT NULL,
                        `interval` INTEGER NOT NULL,
                        `weekdays` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
