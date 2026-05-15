package com.example.lifetogether.domain.model.lists

enum class ListType(val value: String, val displayName: String) {
    ROUTINE("routine", "Routine"),
    WISH_LIST("wish_list", "Wish List"),
    NOTES("notes", "Notes"),
    CHECKLIST("checklist", "Checklist"),
    MEAL_PLANNER("meal_planner", "Meal Planner"),
    ;
}
