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
- [ ] Meal plan recipe Vs custom
  - One should always be null so when changing between custom and recipe, so both aren't saved

### Grocery
- [?] Fix long text layout in Add grocery item.
- [ ] Need more bottom spacing if using double row AddGroceryItem
  - (TODO SOON)
- [ ] _Add an amount field to grocery items_.

### Lists
- [x] Add a more menu for deleting entries in lists overview screen that deletes the whole list and entries.
- [x] Wishlist items with no description should have no extra spacing and no empty text in listdetails.
- [x] Add rename inside list details when not in selection mode
- [x] Checklist should support multiple lines

### Recipes
- [ ] Add a search bar in recipes.
  - Have a clickable search icon
  - when clicked, the topbar text gets empty and instead shows a search field that filters the visible recipes
  - The search should include tags, ingredients and so on so 
    - if searching for a recipe name it should show it
    - if searching for a specific ingredient it should also list those
    - And so on
  - (TODO SOON)
  - _Probably need a new type of recipe card for searching which includes the tag/ingredient etc that got matched in the search so the user knows why it's showing_
- [ ] _Export or share recipes as PDF_.
- [x] Ingredients and instructions need stable IDs.
- [x] When adding ingredients, do not start capitalized
- [x] Recipe servings should be 1 to 100 instead of few predefined
- [x] Ingredient add to grocery list button when grocery suggestion exists
  - Show informational snackbar (version of error snackbar) if already exists on grocery list so it doesn't get added twice from this
  - It can still be added twice from the grocery screen itself though
  - Be able to handle ingredients or suggestions that has a s e.g. banana and bananas should be connected to same
- [ ] Have add to meal plan button on recipes that autofill with that recipe and opens the create meal plan
  - (TODO WOULD LIKE TO IMPLEMENT)
- [x] Recipe card with image (two options)
  - Have prep time below title and image box to the right a bit like routines
  - Or have image behind with gradient from the left with prep and title to the left
- [x] Recipe instructions cannot be read since they should be multiline
- [x] Edit recipe ingredients or instructions
  - Test with husbie changing red sauce to our version
- [x] Make it possible to change order of instructions and ingredients
- [x] Make add instructions field taller to fit two lines

### Guides
- [ ] Add guide manually does not work that well.
- [ ] _Make the UI nicer_.
- [x] OptionRow should not have dividers inside guides.
- [ ] The step player should show whether a step is completed and show the datetime of completion/last edited.
  - (TODO SOON)
- [ ] When changing guide visibility, do not reset progress since progress is private anyway.
  - (TODO SOON)

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
  - (TODO SOON)
- [ ] _Add special day under family settings or the countdown page._
- [x] Clickable area is wrong in personal details

### Setting
- [x] Show app version at the bottom of settings along with user id if logged in
- [ ] How does settings work if not logged in?

### TipTracker
- [ ] Rebuild list of tips (maybe as a card)

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
- [ ] _Traveller_
  - Map with pins for where we have visited, lived and bucket list
  - When clicked it shows bottom sheet with city, country, to-from date, possible to attach album to easily find it again
  - Filter which pins to show (default visited+lived)
  - In album info show if connected to traveller pin and make it clickable

### Navigation
- [ ] Migrate to Navigation 3.
  - (TODO WOULD LIKE TO IMPLEMENT)
- [x] On login, go to the home screen and do not keep any back stack.
- [x] Cannot have multiple profile or settings in back stack. Maybe same for other ones

### Shared architecture and tooling
- [ ] Replace `DropDown.kt` `.menuAnchor` usage because it is deprecated if possible without very complex code.
  - Maybe just rewrite the way to use dropdowns to a more new and native way?
  - (TODO WOULD LIKE TO IMPLEMENT)
- [ ] Try `@PreviewScreenSizes`.
- [ ] The Profiler can see all classes in memory and other things.
- [ ] _Repositories should probably be singletons and stateless._
- [ ] _Use SQLCipher for Room encryption._
- [ ] Error messages should not show when null, instead of keeping both a string and boolean.
  - (TODO SOON) - check if it's not already fine
- [ ] _Make the image download success and version update dialogs nicer._
- [ ] _Show a dialog when there is a new version available._
- [x] Animate when image is loaded and about to be shown e.g. for recipes, routine and such so it doesn't just pop up
- [x] Pressing back on phone should be interupted in selection mode on all screens that have it so it just leaves selection mode instead of navigating back
- [ ] Change how dialogs are shown with enabled parameter instead of "contentState?.showRenameListDialog == true"
  - Maybe even handle some of the events inside instead of many events inside viewmodel per dialog (RequestRenameList, DismissRenameListDialog,RenameListNameChanged,ConfirmRenameList)
  - (TODO SOON)
- [ ] Update libs versions
  - (TODO SOON)
- [ ] Delete things from Converters.kt
  - (TODO SOON)
- [ ] Update sdk to version 37
  - (TODO SOON)

## Icons
- [ ] Make Grocery icon with a plus for the RecipeDetails
- [ ] Make search icon
- [ ] Make meal plan icon
- [ ] Make drag handle icon
- [ ] Make new rounded icons (V2)
- [ ] Make thinner email icon (profile screen email icon should be updated so need to make a new one)