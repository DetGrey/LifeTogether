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
- [ ] All CustomTextField should have first char capitalisation by default
- [ ] Wishlist entries is missing name (only name and priority are required by default). 
  - This is a bug with several ListEntry types where the screen doesn't have all the needed elements
- [ ] A note entry should also be able to add a title (but called itemName)

Here are some questions:

- The AppTopBar is much taller than it used to be. Can it be changed in any way? Might be because of TextDisplayLarge
- ListItem seems too high compared to legacy - is that true?
  - Try bodyMedium + smaller icons first and if not enough then use custom instead of native M3ListItem
- profile screen email icon should be updated so need to make a new one
- I have to check if this is still a problem
  - EditableTextField textfield is not tall enough to handle textStyle = MaterialTheme.typography.displayMedium (should be dynamic)
  - I want it on all screens I think since the skeleton is almost not shown at all
- New note entry should be redesigned and make the textfield be custom with multi-line and other things (box should fill most of the screen)
- Sometimes when creating a new list (note or checklist) it keeps loading for too long when trying to create it and when going back it doesn't show on the listoverview before leaving to homescreen and coming back
- Mealplanner should be redesigned