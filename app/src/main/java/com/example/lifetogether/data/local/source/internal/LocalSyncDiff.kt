package com.example.lifetogether.data.local.source.internal

internal fun <T, K> computeItemsToUpdate(
    currentItems: List<T>,
    incomingItems: List<T>,
    key: (T) -> K,
): List<T> =
    incomingItems.filter { newItem ->
        currentItems.none { currentItem ->
            key(newItem) == key(currentItem) && newItem == currentItem
        }
    }

internal fun <T, K> computeItemsToDelete(
    currentItems: List<T>,
    incomingItems: List<T>,
    key: (T) -> K,
): List<T> =
    currentItems.filter { currentItem ->
        incomingItems.none { newItem -> key(newItem) == key(currentItem) }
    }
