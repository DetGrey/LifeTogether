# Temporary plans

## Family: Show profile images of family members on family screen
Problem:
Family member avatars are not available in the current family sync model. The app only syncs the signed-in user’s full `UserInformation` record; it does not download or cache other users’ profile records locally. Family sync only provides the family document and its `members` list, and `FamilyMember` currently contains only `uid` and `name`, so the family screen has no avatar URL to render.

Solution:
Keep family member display data denormalized inside the family document. Expand each member entry from `uid + name` to `uid + name + imageUrl?`, and update that member `imageUrl` every time the corresponding user updates their profile image. The family screen should render avatars directly from `member.imageUrl` and fall back to the default icon when the URL is missing, the device is offline, or image loading fails. Do not add cross-user profile sync or store other members’ full profile data locally just to support avatars.

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/family/FamilyScreen.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/family/FamilyViewModel.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/profile/ProfileDetails.kt`
- `app/src/main/java/com/example/lifetogether/data/repository/ImageRepositoryImpl.kt`
- `app/src/main/java/com/example/lifetogether/data/remote/FamilyFirestoreDataSource.kt`
- `app/src/main/java/com/example/lifetogether/data/remote/UserFirestoreDataSource.kt`
- `app/src/main/java/com/example/lifetogether/data/repository/UserRepositoryImpl.kt`
- `app/src/main/java/com/example/lifetogether/domain/model/family/FamilyMember.kt`

## Family: Family settings icons are not centered
Problem:
`SettingsItem` places the icon inside a `Box` without `contentAlignment = Alignment.Center`, so the icon sits off-center inside the leading area. The family screen reuses this component, which is why the issue appears there.

Solution:
Fix the generic `SettingsItem` layout so the leading icon area centers its content. Because the component is shared, this should improve both family and settings screens consistently.

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsItem.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/family/FamilyScreen.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsScreen.kt`

## Family: Together since icon should have end padding
Problem:
The `FamilyTogetherSinceRow` adds start padding to the row but no matching end padding, so the edit/clear/save controls sit too close to the right edge.

Solution:
Move the row to symmetric horizontal padding. This is a localized layout fix inside the family screen.

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/family/FamilyScreen.kt`

## Settings: Entire settings card should be clickable, with action sheet when there are multiple actions
Problem:
`SettingsItem` only makes the title and link text clickable separately. That is fragile on touch devices and does not match the requested interaction model. It also hardcodes the idea that an item has at most two inline text actions instead of one row-level action model.

Solution:
Refactor `SettingsItem` to support card-level clicks and optionally multiple actions. For single-action rows, make the full card clickable. For multi-action rows, make the full card clickable and on click open the existing `ActionSheet` and let the screen provide the list of actions (only show actions from that card, not all).

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsItem.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsModels.kt`
- `app/src/main/java/com/example/lifetogether/ui/common/OverflowMenu.kt`

## Admin features: Admin users can grant admin access to another user
Problem:
The app already has a global admin concept, but it is only a hardcoded build-time gate via `BuildConfig.ADMIN_LIST`, currently used to unlock the grocery admin screens. That means admin access cannot be granted or revoked at runtime, and the earlier family-admin approach would be the wrong scope because these screens are app-wide, not family-owned.

Solution:
Keep admin as a global app role and move the source of truth to Firestore. Add one config document such as `app_config/admins` with `adminUids: List<String>`, and expose repository methods to observe, add, and remove admin UIDs. The add flow should take a raw UID, validate it, verify that a user document with that UID exists, and then add it if it is not already present. The screen should show the existing admin UIDs in a list above an `AddNewString` input, with a trailing delete icon per row for revoke. Only existing app admins may grant or revoke access. During rollout, keep `BuildConfig.ADMIN_LIST` as a bootstrap fallback so current admins keep access until the Firestore admin document has been populated; admin checks should temporarily accept either source.

Related files:
- `app/src/main/java/com/example/lifetogether/data/remote/UserFirestoreDataSource.kt`
- `app/src/main/java/com/example/lifetogether/domain/repository/UserRepository.kt`
- `app/src/main/java/com/example/lifetogether/data/repository/UserRepositoryImpl.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/home/HomeRoute.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/profile/ProfileRoute.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsModels.kt`

## Admin features: User ID in settings should be clickable to copy
Problem:
The settings screen renders the user id as plain text, so there is no direct affordance to copy it before using admin tooling or sharing it with another admin.

Solution:
Make the user id row a proper settings action. On tap, copy the id with `copyToClipboard(...)` and show a snackbar or system-style copied confirmation via `UiCommand`. This fits naturally with the broader settings-card refactor above.

Related files:
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/example/lifetogether/ui/feature/settings/SettingsModels.kt`
- `app/src/main/java/com/example/lifetogether/domain/logic/copyToClipboard.kt`

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
