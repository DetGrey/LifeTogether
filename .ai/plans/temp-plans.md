# Temporary plans

## Notifications: Handle multiple FCM tokens per user
Problem:
The family document currently stores a single `fcmToken` per member. That means one device silently overwrites another, token rotation can replace the active token unexpectedly, and logout cleanup removes the whole token field instead of only the current device’s registration. `onNewToken(...)` also only logs today, so token refreshes are not written back immediately.

Solution:
Keep token storage embedded inside each family member entry, but change it from one `fcmToken` string to `fcmTokens: [{ token, lastSeenAt }]`. Token registration should upsert the current device token for the authenticated member, dedupe by token, and refresh `lastSeenAt` whenever that token is successfully registered. Logout should remove only the current device’s token record, not every token for that user. `onNewToken(...)` should immediately register the refreshed token when the user is authenticated and has a family. Notification fan-out should flatten all member token records into the send list. Do not implement pruning yet, but store `lastSeenAt` now so stale tokens can be inspected and cleaned up later if needed.

Related files:
- `app/src/main/java/com/example/lifetogether/data/remote/FamilyFirestoreDataSource.kt`
- `app/src/main/java/com/example/lifetogether/domain/model/family/FamilyMember.kt`
- `app/src/main/java/com/example/lifetogether/domain/repository/UserRepository.kt`
- `app/src/main/java/com/example/lifetogether/data/repository/UserRepositoryImpl.kt`
- `app/src/main/java/com/example/lifetogether/ui/viewmodel/RootCoordinatorViewModel.kt`
- `app/src/main/java/com/example/lifetogether/data/remote/MyFirebaseMessagingService.kt`
- `app/src/main/java/com/example/lifetogether/data/remote/FirebaseAuthDataSource.kt`

## Notifications: Status-bar notification icon is all white on Pixel 8 (TODO)
Problem:
Notifications use `R.drawable.ic_logo` as the small icon. That asset is a full illustrated/logo-style vector with fills, strokes, and internal detail, but Android small notification icons are rendered as monochrome masks in the status bar and notification header. On devices like Pixel 8, the system reduces the icon to a solid white silhouette, so a complex logo turns into an all-white or muddy-looking blob instead of a clean symbol.

Solution:
Separate the notification icons by purpose. 
Keep using the existing branded app icon as the notification `setLargeIcon(...)` image for the expanded/body presentation, but create a new dedicated drawable for `setSmallIcon(...)`. 
That new small icon should be a simple notification-style stencil: 
- one clear shape, 
- transparent background, 
- no gradients, 
- no multiple colors, 
- no photo-like detail, 
- and no thin internal lines that disappear at small sizes. 
Use that dedicated small icon everywhere notifications are built, and set Firebase default notification icon metadata in the manifest so background-delivered notifications use the same asset too.

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/notification/NotificationService.kt`
- `app/src/main/java/com/example/lifetogether/data/remote/MyFirebaseMessagingService.kt`
- `app/src/main/res/drawable/ic_logo.xml`
- `app/src/main/AndroidManifest.xml`
