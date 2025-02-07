package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.grocery.GroceryListNotificationOption

fun groceryListNotificationOptions(
    itemName: String,
    emoji: String,
): GroceryListNotificationOption {
    val options = listOf(
        GroceryListNotificationOption(
            title = "Want happiness? 😁",
            message = "Buy $itemName $emoji right away! 🛒",
        ),
        GroceryListNotificationOption(
            title = "Don’t make me go extinct! 🌍",
            message = "$itemName $emoji needs a forever home. 🏠",
        ),
        GroceryListNotificationOption(
            title = "SOS 🚨",
            message = "$itemName $emoji reporting a serious risk of being forgotten! 😱",
        ),
        GroceryListNotificationOption(
            title = "No more excuses! 🚫",
            message = "$itemName $emoji deserves a ride in your cart. 🛍️",
        ),
        GroceryListNotificationOption(
            title = "Skip me, and you'll... 😬",
            message = "... Truly regret it when it's dinnertime. $itemName $emoji! 🍽️",
        ),
        GroceryListNotificationOption(
            title = "😋",
            message = "$itemName $emoji here, reminding you that I make life tastier. 🍴",
        ),
        GroceryListNotificationOption(
            title = "Match made in grocery heaven! 💖",
            message = "You + $itemName $emoji = a match made in grocery heaven. 😍",
        ),
        GroceryListNotificationOption(
            title = "I don’t want to be dramatic... 🎭",
            message = "... But your kitchen needs me. $itemName $emoji 🔥",
        ),
        GroceryListNotificationOption(
            title = "This isn't just a notification. 🌟",
            message = "It’s destiny. Buy $itemName $emoji. ✨",
        ),
        GroceryListNotificationOption(
            title = "I’m $itemName $emoji! 💥",
            message = " — come and get me before I'm Gon! Not Killua 😉",
        ),
        GroceryListNotificationOption(
            title = "This is your last chance ⚠️",
            message = " — grab $itemName $emoji now or regret it! 😫",
        ),
        GroceryListNotificationOption(
            title = "Failure is not an option! ❌",
            message = "Buy $itemName $emoji immediately. 🚨",
        ),
        GroceryListNotificationOption(
            title = "Danger zone: ⚡️",
            message = "You’re one step away from a grocery catastrophe without $itemName $emoji! 💥",
        ),
        GroceryListNotificationOption(
            title = "Mission critical: 🎯",
            message = "Acquire $itemName $emoji or risk culinary collapse! 🍳",
        ),
        GroceryListNotificationOption(
            title = "Swipe left on indecision. ❌",
            message = "Say yes to $itemName $emoji! ✅",
        ),
        GroceryListNotificationOption(
            title = "Treat yourself 🍰",
            message = " — starting with $itemName $emoji! 🎉",
        ),
    )

    val randomNumber = (Math.random() * options.size).toInt()
    return options[randomNumber]
}
