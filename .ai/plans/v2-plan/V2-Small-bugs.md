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


## Checklist from LifeTogether_TODO.PDF

> Note: All checklist items that are in _italic_ should be ignored as they are optional add-ons that might never be implemented. All bullet points under a checklist in italic should be ignored too

### Meal planner
- [x] Show the recipe as a real recipe card (taken from recipe screen) with an image if one exists, and open the recipe when tapped.
- [x] Keep older meal plans visible while swiping back after creating a new one. All plans should be visible under the day they are added to
- [x] After creating a meal plan, re turn to the week that contains the new entry.
- [x] Fix recipe search in new meal plan entry details. It used to work.
- [x] Meal plan recipe Vs custom
  - One should always be null so when changing between custom and recipe, so both aren't saved
- [x] Meal plan notify button
  - When clicked, show bottom sheet with date and time (default plan date, default time should be 6pm for dinner, 9am for breakfast, noon for lunch and 2pm for snack)
  - On confirm, use firebase to send message or what are my options to make sure it happens on time?
  - Should open the meal plan on click

### Grocery
- [x] Fix long text layout in Add grocery item.
- [x] Need more bottom spacing if using double row AddGroceryItem

### Lists
- [x] Add a more menu for deleting entries in lists overview screen that deletes the whole list and entries.
- [x] Wishlist items with no description should have no extra spacing and no empty text in listdetails.
- [x] Add rename inside list details when not in selection mode
- [x] Checklist should support multiple lines

### Recipes
- [x] Add a search bar in recipes.
  - Have a clickable search icon
  - when clicked, the topbar text gets empty and instead shows a search field that filters the visible recipes
  - The search should include tags, ingredients and so on so 
    - if searching for a recipe name it should show it
    - if searching for a specific ingredient it should also list those
    - And so on
  - _Probably need a new type of recipe card for searching which includes the tag/ingredient etc that got matched in the search so the user knows why it's showing_
- [x] Ingredients and instructions need stable IDs.
- [x] When adding ingredients, do not start capitalized
- [x] Recipe servings should be 1 to 100 instead of few predefined
- [x] Ingredient add to grocery list button when grocery suggestion exists
  - Show informational snackbar (version of error snackbar) if already exists on grocery list so it doesn't get added twice from this
  - It can still be added twice from the grocery screen itself though
  - Be able to handle ingredients or suggestions that has a s e.g. banana and bananas should be connected to same
- [x] Have add to meal plan button on recipes that autofill with that recipe and opens the create meal plan
  - Make it an floating action button maybe?
- [x] Recipe card with image (two options)
  - Have prep time below title and image box to the right a bit like routines
  - Or have image behind with gradient from the left with prep and title to the left
- [x] Recipe instructions cannot be read since they should be multiline
- [x] Edit recipe ingredients or instructions
  - Test with husbie changing red sauce to our version
- [x] Make it possible to change order of instructions and ingredients
- [x] Make add instructions field taller to fit two lines
- [x] Should support ingredient being 0 when saving like it used to

### Guides
- [x] OptionRow should not have dividers inside guides.
- [x] Add guide manually does not work that well.
- [x] When changing guide visibility, do not reset progress since progress is private anyway.

### Gallery media
- [x] The uploaded images should keep the original quality
- [x] For videos, query `MediaStore.Video.Media.DATE_TAKEN` and `MediaStore.Video.Media.DATE_MODIFIED`. 
  - This is a recommended change if you want accurate MediaStore dates for videos.
- [x] Figure out why downloaded videos show the created-at date in gallery while images show the download date.
- [x] Decide what should happen when `updateGalleryMedia()` download fails.

### Family and relationship features
- [x] Clickable area is wrong in personal details
- [x] Change x days together so it is only shown if a date is added to family info.
  - This means that a new variable should be added to the data class

### Setting
- [x] Show app version at the bottom of settings along with user id if logged in

### TipTracker
- [x] Rebuild list of tips (maybe as a card)

### Navigation
- [x] Migrate to Navigation 3.
- [x] On login, go to the home screen and do not keep any back stack.
- [x] Cannot have multiple profile or settings in back stack. Maybe same for other ones

### Shared architecture and tooling
- [x] Animate when image is loaded and about to be shown e.g. for recipes, routine and such so it doesn't just pop up
- [x] Pressing back on phone should be interupted in selection mode on all screens that have it so it just leaves selection mode instead of navigating back
- [x] Update libs versions
- [x] Update sdk to version 37
- [x] Fix settings.gradle.kts warnings
- [x] Repositories should probably be singletons and stateless.
- [x] Change how dialogs are shown with enabled parameter instead of "contentState?.showRenameListDialog == true"
  - Maybe even handle some of the events inside instead of many events inside viewmodel per dialog (RequestRenameList, DismissRenameListDialog,RenameListNameChanged,ConfirmRenameList)

## Icons
- [x] Make Grocery icon with a plus for the RecipeDetails
- [x] Make search icon
- [x] Make close (X) button
- [x] Make meal plan icon 
- [x] Make drag handle icon
- [x] Make new rounded icons (V2)
- [x] Make thinner email icon (profile screen email icon should be updated so need to make a new one)
