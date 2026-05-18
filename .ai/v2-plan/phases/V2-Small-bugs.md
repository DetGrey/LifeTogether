# V2 Small bugs

Here are some bugs that need to be fixed:

- [x] recipe tags order should go back the old way from before v2 and refactoring started (with all, simple, dinner, breakfast being the first ones or something similar to that)
- [x] profile screen password icon looks different from when I open the icon (maybe it uses androidx.credentials:credentials:1.2.0-rc01@aar instead of my custom one)
- [x] recipeDetailsmodels should not have default/null values unless it's actually needed. Same for all other models that do the same
- [x] in recipedetailsscreen when creating new recipe it shows "0" in preparation time from the start but it should be empty until user adds something
- [x] AddNewIngredient should not have fixed height but still look around the same with dynamic
- [x] Make the Surface for MediaDetailsPanelContent in MediaDetailsScreen easier to dismiss (often fails and moved up again when I do a natural swipe)
- [x] In ConfirmationDialogWithTextField make the textfield in auto focus so it opens keyboard
- [x] Remove all default values from Content UiStates unless absolutely necessary
- [x] Wishlist entries is missing name (only name and priority are required by default).
- [x] A note entry should also be able to add a title (but called itemName)
- [x] Weird spacing between the featurecards on homescreen
- [x] Some recipes do not show anymore. Make a log for all items that are now filtered away from firestore to see if any had bad format

Here are some questions:

- The AppTopBar is much taller than it used to be. Can it be changed in any way? Might be because of TextDisplayLarge
- ListItem seems too high compared to legacy - is that true?
  - Go back to use custom instead of native M3ListItem
- profile screen email icon should be updated so need to make a new one
- I have to check if this is still a problem
  - EditableTextField textfield is not tall enough to handle textStyle = MaterialTheme.typography.displayMedium (should be dynamic)
  - I want it on all screens I think since the skeleton is almost not shown at all
- New note entry should be redesigned and make the textfield be custom with multi-line and other things (box should fill most of the screen)
- Sometimes when creating a new list (note or checklist) it keeps loading for too long when trying to create it and when going back it doesn't show on the listoverview before leaving to homescreen and coming back
- Mealplanner should be redesigned
  - itemName should either be the recipe name or the custom mealname

## Checklist from LifeTogether_TODO.PDF

> Note: All checklist items that are in _italic_ should be ignored as they are optional add-ons that might never be implemented. All bullet points under a checklist in italic should be ignored too

### Meal planner
- [x] Show the recipe as a real recipe card (taken from recipe screen) with an image if one exists, and open the recipe when tapped.
- [x] Keep older meal plans visible while swiping back after creating a new one. All plans should be visible under the day they are added to
- [x] After creating a meal plan, re turn to the week that contains the new entry.
- [x] Fix recipe search in new meal plan entry details. It used to work.

### Grocery
- [ ] Fix long text layout in Add grocery item.
- [ ] _Add an amount field to grocery items_.

### Lists
- [ ] Add a more menu for deleting entries in lists overview screen that deletes the whole list and entries.
- [ ] Wishlist items with no description should have no extra spacing and no empty text in listdetails.

### Recipes
- [ ] Add a search bar in recipes.
  - Have a clickable search icon
  - when clicked, the topbar text gets empty and instead shows a search field that filters the visible recipes
  - The search should include tags, ingredients and so on so 
    - if searching for a recipe name it should show it
    - if searching for a specific ingredient it should also list those
    - And so on
  - _Probably need a new type of recipe card for searching which includes the tag/ingredient etc that got matched in the search so the user knows why it's showing_
- [ ] _Export or share recipes as PDF_.
- [ ] _Ingredients and instructions need stable IDs_.

### Guides
- [ ] Add guide manually does not work that well.
- [ ] _Make the UI nicer_.
- [ ] OptionRow should not have dividers inside guides.
- [ ] Make the step content text inside the step player bigger.
- [ ] The step player should show whether a step is completed and show the datetime of completion/last edited.
- [ ] When changing guide visibility, do not reset progress since progress is private anyway.

### Search and discovery
- [ ] _Add filtering for all, notes, guides, and lists_.
- [ ] _Add a Home screen box for essentials_.
  - Inside essentials, find notes, lists, and guides with a filter at the top.
  - Add search to find features.
  - Search guides to find the guides page, or search more specifically for items like a WiFi note.

### Gallery media
- [ ] _Add tags_.
- [ ] Add a note or description.
- [ ] Share media.
- [ ] When adding gallery videos, handle the video thumbnail fallback better if thumbnail generation fails.
- [ ] For videos, query `MediaStore.Video.Media.DATE_TAKEN` and `MediaStore.Video.Media.DATE_MODIFIED`.
- [ ] Decide what should happen when `updateGalleryMedia()` download fails.
- [ ] Figure out why downloaded videos show the created-at date in gallery while images show the download date.

### Gallery albums
- [ ] _Send a notification when everything is downloaded, and maybe link to the gallery LifeTogether folder if possible._

### Family and relationship features
- [ ] _Add a love button that sends a loving notification to all other family members except yourself._
  - Disable the love button for 30 seconds after click to avoid spamming.
  - Use predefined messages in code instead of Firestore to reduce fetching.
- [ ] Change x days together so it is only shown if a date is added to family info.
  - This means that a new variable should be added to the data class
- [ ] _Add special day under family settings or the countdown page._
- [ ] On login, go to the home screen and do not keep any back stack.

### Family memories and widgets
- [ ] _Add likes and dislikes for food and everything else._
- [ ] _Add countdowns for special days._
- [ ] _Add love notes._
- [ ] _Add relationship goals, dares, trips, and gifts._
- [ ] _Add a bucket list together._
  - Could be a user list checklist
- [ ] _Add a diary together._
- [ ] _Add a time capsule with letters, voice, and pictures that can be locked and unlocked on specific dates._
- [ ] _Add travel goals for countries to visit and see._
  - This could just be part of a user list
- [ ] _Add mood-based suggestions for when down, sad, or exhausted._

### Navigation
- [ ] Migrate to Navigation 3.

### Shared architecture and tooling
- [ ] Replace `DropDown.kt` `.menuAnchor` usage because it is deprecated if possible without very complex code.
  - Maybe just rewrite the way to use dropdowns to a more new and native way?
- [ ] Try `@PreviewScreenSizes`.
- [ ] The Profiler can see all classes in memory and other things.
- [ ] _Repositories should probably be singletons and stateless._
- [ ] _Use SQLCipher for Room encryption._
- [ ] Error messages should not show when null, instead of keeping both a string and boolean.
- [ ] _Make the image download success and version update dialogs nicer._
- [ ] _Show a dialog when there is a new version available._
- [ ] Animate when image is loaded and about to be shown e.g. for recipes and such so it doesn't just pop up
- [ ] Pressing back on phone should be interupted in selection mode on all screens that have it so it just leaves selection mode instead of navigating back


## Shelved changes:
Fixing clickable area in ProfileDetails.kt

Trying out two-row AddNewListItem.kt

WishesSection.kt spacing fixed

CompletableListItem.kt can have multiline (should fix instructions)