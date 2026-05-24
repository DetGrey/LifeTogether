package com.example.lifetogether.domain.notification

import android.util.Log
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.MealNotificationPreferencesRepository
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanAlarmOrchestrator @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val mealPlannerRepository: MealPlannerRepository,
    private val recipeRepository: RecipeRepository,
    private val prefsRepository: MealNotificationPreferencesRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    private companion object {
        const val TAG = "MealPlanAlarmOrch"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observationJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            combine(
                sessionRepository.sessionState
                    .map { it.authenticatedUserOrNull?.familyId }
                    .distinctUntilChanged(),
                prefsRepository.observePreferences(),
            ) { familyId, prefs -> Pair(familyId, prefs) }
                .flatMapLatest { (familyId, prefs) ->
                    if (familyId == null || !prefs.masterEnabled) {
                        flowOf(Triple(emptyList<MealPlan>(), prefs, emptyMap<String, Int>()))
                    } else {
                        combine(
                            mealPlannerRepository.observeMealPlans(familyId),
                            recipeRepository.observeRecipes(familyId),
                        ) { mealsResult, recipesResult ->
                            val meals = (mealsResult as? Result.Success)?.data ?: emptyList()
                            val prepTimes = (recipesResult as? Result.Success)?.data
                                ?.associate { it.id to it.preparationTimeMin }
                                ?: emptyMap()
                            Triple(meals, prefs, prepTimes)
                        }
                    }
                }
                .collect { (mealPlans, prefs, prepTimes) ->
                    alarmScheduler.scheduleAll(mealPlans, prefs, prepTimes)
                }
        }
        Log.d(TAG, "Started alarm observation")
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
        alarmScheduler.cancelAll()
        Log.d(TAG, "Stopped alarm observation and cancelled all alarms")
    }

    suspend fun rescheduleOnce() {
        val session = sessionRepository.sessionState.first()
        val prefs = prefsRepository.observePreferences().first()
        val familyId = (session as? SessionState.Authenticated)?.user?.familyId

        if (familyId == null || !prefs.masterEnabled) {
            alarmScheduler.scheduleAll(emptyList(), prefs, emptyMap())
            return
        }

        val mealPlans = mealPlannerRepository.observeMealPlans(familyId).first()
            .let { (it as? Result.Success)?.data ?: emptyList() }

        val prepTimes = recipeRepository.observeRecipes(familyId).first()
            .let { (it as? Result.Success)?.data?.associate { r -> r.id to r.preparationTimeMin } ?: emptyMap() }

        alarmScheduler.scheduleAll(mealPlans, prefs, prepTimes)
        Log.d(TAG, "One-shot reschedule complete")
    }
}
