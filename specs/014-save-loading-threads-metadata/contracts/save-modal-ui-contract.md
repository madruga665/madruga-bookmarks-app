# Contract: Save Bookmark Modal UI & Button Loading State

## Component Specification: `SaveBookmarkBottomSheet`

### Interface & Parameters
```kotlin
@Composable
fun SaveBookmarkBottomSheet(
    uiState: SaveBookmarkModalUiState,
    onCollectionSelect: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleCreateFolder: () -> Unit,
    onNewFolderNameChange: (String) -> Unit,
    onNewFolderColorSelect: (String) -> Unit,
    onCreateFolderSubmit: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit,
    collectionRepository: CollectionRepository? = null,
    onTagInputChange: (String) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    modifier: Modifier = Modifier
)
```

### Save Button Behavior Contract
1. **Disabled & Non-clickable**: When `uiState.isSaving == true`, `NeobrutalistButton` MUST pass `enabled = false`.
2. **Visual Progress Indicator**: When `uiState.isSaving == true`, the button content slot MUST render `CircularProgressIndicator` with:
   - Size: `24.dp`
   - Stroke width: `2.5.dp`
   - Color: `Color.Black` (or `NeobrutalismTheme.colors.border`)
   - Test Tag: `"tag_save_bookmark_loading_spinner"`
3. **Fixed Dimensions**: The confirmation button MUST maintain a consistent minimum height of `52.dp` and `fillMaxWidth()`, preventing vertical bounce when alternating between text and spinner.
4. **Re-entrance Protection in ViewModel**:
   ```kotlin
   fun onConfirmSave(onSuccess: () -> Unit) {
       val current = _uiState.value
       if (current.isSaving || current.targetUrl.isBlank()) return
       // proceed with save
   }
   ```
