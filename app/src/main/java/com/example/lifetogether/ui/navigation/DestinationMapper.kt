package com.example.lifetogether.ui.navigation

fun routeFromDestinationString(destination: String): AppRoute =
    NotificationDestination.fromKey(destination)?.toAppRoute() ?: HomeNavRoute
