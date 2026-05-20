package com.example.lifetogether.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `recipe_ingredients`")
        db.execSQL("DROP TABLE IF EXISTS `recipe_instructions`")
        db.execSQL("DROP TABLE IF EXISTS `recipes`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recipes` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `last_updated` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `preparation_time_min` INTEGER NOT NULL,
                `favourite` INTEGER NOT NULL,
                `servings` INTEGER NOT NULL,
                `tags` TEXT NOT NULL,
                `image_data` BLOB,
                `image_url` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                `id` TEXT NOT NULL,
                `recipe_id` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `amount` REAL NOT NULL,
                `measure_type` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipe_id` ON `recipe_ingredients` (`recipe_id`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recipe_instructions` (
                `id` TEXT NOT NULL,
                `recipe_id` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `item_name` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_instructions_recipe_id` ON `recipe_instructions` (`recipe_id`)")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `image_url` TEXT")
        db.execSQL("ALTER TABLE `families` ADD COLUMN `image_url` TEXT")
        db.execSQL("ALTER TABLE `recipes` ADD COLUMN `image_url` TEXT")
        db.execSQL("ALTER TABLE `list_entries_routine` ADD COLUMN `image_url` TEXT")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `user_lists` WHERE `type` = 'MEAL_PLANNER'")
        db.execSQL("DROP TABLE IF EXISTS `list_entries_meal_plan`")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meal_plans` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `recipe_id` TEXT,
                `custom_meal_name` TEXT,
                `meal_type` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `last_updated` INTEGER NOT NULL,
                `date_created` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `list_entries_wish` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `last_updated` INTEGER,
                `date_created` INTEGER,
                `is_purchased` INTEGER NOT NULL,
                `url` TEXT,
                `estimated_price_minor` INTEGER,
                `currency_code` TEXT,
                `priority` TEXT NOT NULL,
                `notes` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `list_entries_notes` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `markdown_body` TEXT NOT NULL,
                `last_updated` INTEGER,
                `date_created` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `list_entries_checklist` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `is_checked` INTEGER NOT NULL,
                `last_updated` INTEGER,
                `date_created` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `list_entries_meal_plan` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `recipe_id` TEXT,
                `custom_meal_name` TEXT,
                `last_updated` INTEGER,
                `date_created` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

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
            """
            CREATE TABLE IF NOT EXISTS `list_entries_routine` (
                `id` TEXT NOT NULL,
                `family_id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_name` TEXT NOT NULL,
                `image_data` BLOB,
                `last_updated` INTEGER,
                `date_created` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
