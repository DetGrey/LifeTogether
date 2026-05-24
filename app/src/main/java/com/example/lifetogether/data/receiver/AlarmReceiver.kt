package com.example.lifetogether.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.ui.navigation.NotificationDestination
import com.example.lifetogether.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.lifetogether.ACTION_MEAL_PLAN_ALARM") return

        val mealPlanId = intent.getStringExtra("meal_plan_id") ?: return
        val mealName = intent.getStringExtra("meal_name") ?: "Meal"
        val mealType = MealType.fromValue(intent.getStringExtra("meal_type")) ?: MealType.DINNER
        val prepTimeMin = intent.getIntExtra("prep_time_min", 0)
        val notes = intent.getStringExtra("notes").orEmpty()

        val body = buildBody(mealType, prepTimeMin)
        val bigText = if (notes.isNotBlank()) "$body\n📝 $notes" else body

        notificationService.createNotification(
            channelId = Constants.MEAL_PLAN_CHANNEL,
            title = buildTitle(mealType, mealName),
            message = body,
            bigText = bigText,
            category = NotificationCompat.CATEGORY_REMINDER,
            priority = NotificationCompat.PRIORITY_HIGH,
            notificationId = mealPlanId.hashCode(),
            destination = NotificationDestination.MealPlan(mealPlanId),
        )
    }

    private fun buildTitle(mealType: MealType, mealName: String): String {
        return "${mealType.displayName}: $mealName"
    }
    private fun buildBody(mealType: MealType, prepTimeMin: Int): String {
        val base = "${mealType.displayName} time is coming up"
        return if (prepTimeMin > 0) "$base — prep time is ${formatPrepTime(prepTimeMin)}" else base
    }

    private fun formatPrepTime(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}min"
    }
}
