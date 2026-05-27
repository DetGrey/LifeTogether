# Temporary plans

- [ ] The album thumbnail should show the newest media by dateCreated

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
