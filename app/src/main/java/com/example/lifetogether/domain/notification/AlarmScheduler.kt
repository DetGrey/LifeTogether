package com.example.lifetogether.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lifetogether.data.receiver.AlarmReceiver
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences
import com.example.lifetogether.domain.model.notification.isEnabledFor
import com.example.lifetogether.domain.model.notification.timeFor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val TAG = "AlarmScheduler"
        const val ACTION_MEAL_PLAN_ALARM = "com.example.lifetogether.ACTION_MEAL_PLAN_ALARM"
        const val EXTRA_MEAL_PLAN_ID = "meal_plan_id"
        const val EXTRA_MEAL_NAME = "meal_name"
        const val EXTRA_MEAL_TYPE = "meal_type"
        const val EXTRA_PREP_TIME_MIN = "prep_time_min"
        const val EXTRA_NOTES = "notes"
    }

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val scheduledIds = mutableSetOf<Int>()

    @Synchronized
    fun scheduleAll(mealPlans: List<MealPlan>, prefs: MealNotificationPreferences, prepTimes: Map<String, Int> = emptyMap()) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        val toSchedule = if (!prefs.masterEnabled) {
            emptyList()
        } else {
            mealPlans.filter { meal ->
                val date = runCatching { LocalDate.parse(meal.date) }.getOrNull() ?: return@filter false
                if (date < today) return@filter false
                if (!prefs.isEnabledFor(meal.mealType)) return@filter false
                val (hour, minute) = prefs.timeFor(meal.mealType)
                val alarmTime = date.atTime(hour, minute)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                alarmTime > now
            }
        }

        val desiredIds = toSchedule.map { it.id.hashCode() }.toSet()

        (scheduledIds - desiredIds).forEach { id -> cancelById(id) }

        if (prefs.masterEnabled) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted — skipping scheduling")
                scheduledIds.clear()
                return
            }
            toSchedule.forEach { meal -> scheduleAlarm(meal, prefs, meal.recipeId?.let { prepTimes[it] }) }
        }

        scheduledIds.clear()
        scheduledIds.addAll(desiredIds)
        Log.d(TAG, "Scheduled ${desiredIds.size} meal plan alarms")
    }

    @Synchronized
    fun cancelAll() {
        scheduledIds.forEach { id -> cancelById(id) }
        scheduledIds.clear()
        Log.d(TAG, "Cancelled all meal plan alarms")
    }

    private fun cancelById(id: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MEAL_PLAN_ALARM
        }
        val pi = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pi?.let { alarmManager.cancel(it) }
    }

    private fun scheduleAlarm(meal: MealPlan, prefs: MealNotificationPreferences, prepTimeMin: Int?) {
        val date = runCatching { LocalDate.parse(meal.date) }.getOrNull() ?: return
        val (hour, minute) = prefs.timeFor(meal.mealType)
        val alarmTimeMillis = date.atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MEAL_PLAN_ALARM
            putExtra(EXTRA_MEAL_PLAN_ID, meal.id)
            putExtra(EXTRA_MEAL_NAME, meal.itemName)
            putExtra(EXTRA_MEAL_TYPE, meal.mealType.name)
            putExtra(EXTRA_PREP_TIME_MIN, prepTimeMin)
            putExtra(EXTRA_NOTES, meal.notes)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            meal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pi)
    }
}

val NOTIFIABLE_MEAL_TYPES = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
