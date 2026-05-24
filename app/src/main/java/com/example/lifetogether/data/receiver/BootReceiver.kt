package com.example.lifetogether.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifetogether.domain.notification.MealPlanAlarmOrchestrator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmOrchestrator: MealPlanAlarmOrchestrator

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmOrchestrator.rescheduleOnce()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
