# V2 Small bugs

Here are some bugs that need to be fixed:

- [x] recipe tags order should go back the old way from before v2 and refactoring started (with all, simple, dinner, breakfast being the first ones or something similar to that)
- [ ] profile screen password icon looks different from when I open the icon (maybe it uses androidx.credentials:credentials:1.2.0-rc01@aar instead of my custom one)
- [x] recipeDetailsmodels should not have default/null values unless it's actually needed. Same for all other models that do the same
- [x] in recipedetailsscreen when creating new recipe it shows "0" in preparation time from the start but it should be empty until user adds something
- [x] AddNewIngredient should not have fixed height but still look around the same with dynamic
- [x] Make the Surface for MediaDetailsPanelContent in MediaDetailsScreen easier to dismiss (often fails and moved up again when I do a natural swipe)
- [x] In ConfirmationDialogWithTextField make the textfield in auto focus so it opens keyboard
- [ ] Remove all default values from Content UiStates unless absolutely necessary

Here are some questions:

- The AppTopBar is much taller than it used to be. Can it be changed in any way? Might be because of TextDisplayLarge
- ListItem seems too high compared to legacy - is that true?
  - Try bodyMedium + smaller icons first and if not enough then use custom instead of native M3ListItem
- It's kinda laggy when navigating to a new screen - maybe from the switch between loading skeleton and ui content? What can be done?
  - ListDetailsScreen, ListEntryDetailsScreen and RecipeDetailsScreen has been changed to try it out
- profile screen email icon should be updated so need to make a new one
- I have to check if this is still a problem
  - EditableTextField textfield is not tall enough to handle textStyle = MaterialTheme.typography.displayMedium (should be dynamic)
  - I want it on all screens I think since the skeleton is almost not shown at all