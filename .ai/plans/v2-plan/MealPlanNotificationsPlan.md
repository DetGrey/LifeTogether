# Meal Plan Notifications Plan

## Status

Agreed through grill-me interview. Planning only, no implementation included.

## Goal

Add scheduled meal plan reminders that fire at configurable times per meal type, managed entirely on-device via AlarmManager. Each partner controls their own notification preferences independently.

## What Changes

- Remove the per-meal "notify" button from meal plan entries
- Add an overflow menu to `MealPlannerScreen` (same `ic_overflow_menu` + `ActionSheet` pattern as `ListsScreen`) with a "Notification settings" item
- Wire the existing "Manage notifications" stub in `SettingsScreen` to navigate to the same Notification Settings screen
- Build a Notification Settings screen with per-meal-type toggles and time pickers
- Schedule local exact alarms via `AlarmManager`; reschedule on data change and device reboot
- Add a first-visit onboarding prompt on the Meal Planner screen

## Decisions

### Notification mechanism
`AlarmManager.setExactAndAllowWhileIdle()` — local, on-device, exact to the second. No server-side scheduling, no Firebase Cloud Functions. Each device manages its own alarms independently.

Requires two permissions declared in the manifest:
- `SCHEDULE_EXACT_ALARM` (Android 12+) — special permission, user grants via Settings → Special app access → Alarms & reminders
- `POST_NOTIFICATIONS` (Android 13+) — normal runtime permission, requested in-app
- `RECEIVE_BOOT_COMPLETED` — for the boot receiver

### Who gets notified
Per-person. Each partner sets their own preferences on their own device. One partner changing their reminder time has no effect on the other.

### Preference storage
DataStore (local, on-device). Not Firestore. Notification preferences are device preferences, not user data. No cross-device sync needed.

### Rescheduling triggers
Three events trigger a full alarm sync (diff Room meal plans + DataStore prefs → add/cancel alarms):

1. **Notification settings change** — any toggle or time change in the settings screen
2. **Meal plan Room flow emission** — covers both local UI writes and remote Firestore sync; no distinction needed since all data lands in Room first
3. **Device reboot** — `BOOT_COMPLETED` BroadcastReceiver re-derives alarms from Room + DataStore without opening the app

No dedicated `ScheduledAlarm` Room table. Alarms are always re-derived from the source data.

### Boot receiver behaviour
The `BOOT_COMPLETED` receiver fires without the app ever being opened. The system starts a short-lived process, the receiver reads Room and DataStore (both are files on disk), re-registers all future alarms, and exits. No Activity or ViewModel involved. Exception: if the user has force-stopped the app via Android Settings, no broadcasts are delivered — this is acceptable Android-wide behaviour.

### Grocery notifications
Untouched. Grocery notifications are manual FCM pushes (user taps a bell on a grocery item to push to their partner immediately). Nothing to configure. System notification channel settings (via the deep-link in Notification Settings) covers muting grocery if the user wants.

## Notification Settings Screen

Entry points:
- Overflow menu on `MealPlannerScreen` → "Notification settings"
- "Manage notifications" item in `SettingsScreen`

Layout:

```
Meal Plan Notifications

[toggle]  Meal plan reminders        ON/OFF   ← master switch

Reminder times
[toggle]  Breakfast        9:00 AM            ← tap time → time picker
[toggle]  Lunch           12:00 PM
[toggle]  Dinner           6:00 PM
[toggle]  Snack            2:00 PM

─────────────────────────────────────────
Manage notification channels →             ← deep-links to Android system settings
```

- Master toggle turns all meal alarms off without losing the individual time/toggle settings
- Per-type toggle enables/disables alarms for that meal type only
- Tapping a time row opens a time picker; the new time takes effect for all future alarms of that type immediately
- "Manage notification channels" opens `Settings → App → Notifications` (the standard Android deep-link) — covers channel-level mute, sound, badges for both meal plan and grocery channels

## Onboarding Prompt

Shown **once**, the **first time the user navigates to the Meal Planner screen** (not on cold app launch). Stored as a boolean in DataStore (`mealPlanNotificationsOnboardingShown`).

Flow:
1. User opens Meal Planner for the first time
2. Bottom sheet appears: *"Want reminders for your meals? We'll notify you before each meal — breakfast at 9am, dinner at 6pm, etc. You can customise times in settings."*
3. Two actions: **Enable** / **Not now**
4. If **Enable**:
   - Request `POST_NOTIFICATIONS` runtime permission (Android 13+)
   - If granted: open Alarms & Reminders system settings for `SCHEDULE_EXACT_ALARM`
   - On return to app: if both permissions granted, enable master toggle in DataStore and schedule alarms for all future meal plan entries
5. If **Not now**: dismiss, mark onboarding as shown, user can enable later from settings
6. Either path marks `mealPlanNotificationsOnboardingShown = true` so the prompt never appears again

## Default Reminder Times

| Meal type | Default time |
|-----------|-------------|
| Breakfast | 09:00       |
| Lunch     | 12:00       |
| Dinner    | 18:00       |
| Snack     | 14:00       |

## DataStore Schema

```
mealPlanNotificationsEnabled: Boolean       // master toggle
mealPlanNotificationsOnboardingShown: Boolean
breakfastNotificationsEnabled: Boolean
lunchNotificationsEnabled: Boolean
dinnerNotificationsEnabled: Boolean
snackNotificationsEnabled: Boolean
breakfastReminderHour: Int                  // 0–23
breakfastReminderMinute: Int                // 0–59
lunchReminderHour: Int
lunchReminderMinute: Int
dinnerReminderHour: Int
dinnerReminderMinute: Int
snackReminderHour: Int
snackReminderMinute: Int
```

## AlarmManager Integration

- One alarm per future meal plan entry where the meal type is enabled
- Alarm time = `meal.date` + `reminderHour:reminderMinute` for that meal's type
- Past meal plan entries are never scheduled
- Alarm `requestCode` / `notificationId` derived from `mealPlanId` (stable, survives reschedule)
- On alarm fire: `AlarmReceiver` posts a local notification via `NotificationService.createNotification()` with destination `NotificationDestination.MealPlan` (new entry to add alongside existing `Grocery`)
- Notification taps open the Meal Planner screen

## Notification Channel

Add `MEAL_PLAN_CHANNEL = "meal_plan_notification_channel"` to `Constants`. Register it in `NotificationService.addNotificationChannels()` alongside the existing grocery and default channels.

## Open Questions

None. All branches resolved.
