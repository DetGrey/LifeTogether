## --- Meal planner ---

## --- Grocery ---

### Optional
- [ ] Add an amount field to grocery items.

## --- Lists ---
- [x] Move priority up under name in wish list editor
- [x] Cannot check off an item on wishlist (some bug that resets the completable toggle or something)
- [x] Checklist/wish list purchased section icon is too big
- [x] Bottom Padding bug in checklist (too much padding between the add card and list items since there shouldn't be any)

## --- Recipes ---

### Optional
- [ ] Export or share recipes as PDF.
-
## --- Guides ---
- [x] Add a "complete and go to step X" for guides so when you add a new guide or have a new device, you can easily update the progress
  - This should be in the overflow/action menu in the guidedetailsscreen

### Optional
- [ ] Make the UI nicer.
- [ ] The step player should show whether a step is completed and show the datetime of completion/last edited.

## --- Gallery media ---
- [x] The gallery media title does not change date when swiping between images

### Optional
- [ ] Add tags.
- [ ] Add a note or description.
- [ ] When adding gallery videos, handle the video thumbnail fallback better if thumbnail generation fails.
- [ ] Share media.

## --- Gallery albums ---

### Optional
- [ ] Send a notification when everything is downloaded, and maybe link to the gallery LifeTogether folder if possible.

## --- Family and relationship features ---
- [x] Show profile image of family members on family screen instead of the icon
- [x] Family and settings icons are not centered
- [x] Together since icon on family screen should also have end padding

### Optional
- [ ] Add special day under family settings or the countdown page.
- [ ] Add a love button that sends a loving notification to all other family members except yourself.
    - Disable the love button for 30 seconds after click to avoid spamming.
    - Use predefined messages in code instead of Firestore to reduce fetching.

## --- Setting ---
- [x] Settings card should be clickable, not just the text
  - When there are multiple, show a action sheet with the options on click

## --- TipTracker ---

## --- Admin features ---
- [ ] An admin user should be able to add another user as admin
  - This also means that the user id in settings should be clickable to copy it with a snackbar or android default message saying it's copied

### Optional
- [ ] Add likes and dislikes for food and everything else.
- [ ] Add countdowns for special days.
- [ ] Add love notes.
- [ ] Add relationship goals, dares, trips, and gifts.
- [ ] Add a bucket list together.
    - Could be a user list checklist
- [ ] Add a diary together.
- [ ] Add a time capsule with letters, voice, and pictures that can be locked and unlocked on specific dates.
- [ ] Add travel goals for countries to visit and see.
    - This could just be part of a user list
- [ ] Add mood-based suggestions for when down, sad, or exhausted.
- [ ] Traveller
    - Map with pins for where we have visited, lived and bucket list
    - When clicked it shows bottom sheet with city, country, to-from date, possible to attach album to easily find it again
    - Filter which pins to show (default visited+lived)
    - In album info show if connected to traveller pin and make it clickable

## --- Shared architecture and tooling ---

### Optional
- [ ] Use SQLCipher for Room encryption.
- [ ] Make the image download success and version update dialogs nicer.
- [ ] Show a dialog when there is a new version available.
- [ ] Replace `DropDown.kt` `.menuAnchor` usage because it is deprecated if possible without very complex code.
    - Maybe just rewrite the way to use dropdowns to a more new and native way?
    - (TODO WOULD LIKE TO IMPLEMENT)
- [ ] Use R8 (kotlin something)
- [ ] Update Claude file and other to use Android CLI
- [ ] Think about if we really wanna catch all exceptions or if it's better to let it crash so we notice and can fix all the bugs

## --- Notifications ---
- [ ] Handle having multiple fcm tokens per user
- [ ] Notification icon is all white on pixel 8
  - Shows correctly on lock screen but not in top bar when phone is on
  - Happens with some other apps too so there is probably something we should have added to prevent it

## --- Icons ---

## --- Tooling to try out ---
- Try `@PreviewScreenSizes`.
- The Profiler can see all classes in memory and other things.
- Check android bench for best AI for kotlin coding


# About using R8 app optimization

R8 is already half-configured in your build — `proguardFiles` is set, but `isMinifyEnabled = false` means it never actually runs. Enabling it is a one-line change but needs careful verification.

**What you'd gain:**
- Smaller APK — Compose, Hilt, Firestore all pull in a lot of code your app doesn't use
- Faster cold startup — dead bytecode is never loaded or JIT-compiled
- Less memory pressure at runtime

**Why it's safe for most of your stack:**
Hilt, Compose, Firebase, Coroutines, and Kotlinx Serialization all ship their own consumer ProGuard rules inside their AARs, so R8 already knows what to keep for those. You don't have to write rules for them.

**Where you'd need to verify carefully:**
1. **`@Serializable` routes** (`NavRoutes.kt`) — kotlinx.serialization ships rules, but the generated serializers for your specific classes need to survive. If the back stack restoration breaks in a release build, this is the first place to look.
2. **`@AssistedFactory` interfaces** — the generated Hilt factory implementations. These are reflectively looked up; if R8 strips them the app crashes at the `hiltViewModel { ... }` call site.
3. **Any reflection in your data layer** — Firestore's document mapping uses reflection; it ships keep rules but custom model classes sometimes need `@Keep`.

**Honest assessment for this app:**
It's worth doing, but the risk is that bugs only appear in release builds (debug stays unminified). You'd need to run through the main flows in a release build after enabling it, and be prepared to add keep rules to `proguard-rules.pro` when things break. For a personal app where you control the release cadence, that's manageable — but it's not a one-line change in practice, it's a one-line change plus a QA pass of the release build.

If you want to do it, the change is:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    // rest unchanged
}
```

Then build a release APK and test it end-to-end before shipping.