# V2 Small bugs

Here are some bugs that need to be fixed.

- [ ] recipe tags order should go back the old way
- [ ] profile screen email icon should be updated
- [ ] profile screen password icon looks different from when I open the icon (maybe it uses androidx.credentials:credentials:1.2.0-rc01@aar instead of my custom one)
- [ ] recipeDetailsmodels should not have default/null values unless it's actually needed. Same for all other models that do the same
- [ ] in recipedetailsscreen when creating new recipe it shows 0 in preparation time from the start but it should be empty until user adds something
- [ ] EditableTextField textfield is not tall enough to handle textStyle = MaterialTheme.typography.displayMedium (should be dynamic)
- [ ] AddNewIngredient should not have fixed height but still look around the same with dynamic
- [ ] Make ActionSheet easier to dismiss (often fails and moved up again when I do a natural swipe)
- [ ] In ConfirmationDialogWithTextField make the textfield in auto focus so it opens keyboard

Here are some questions

- The AppTopBar is much taller than it used to be. Can it be changed in any way?
- ListItem seems too high compared to legacy - is that true?
- It's kinda laggy when navigating to a new screen - maybe from the switch between loading skeleton and ui content? What can be done?