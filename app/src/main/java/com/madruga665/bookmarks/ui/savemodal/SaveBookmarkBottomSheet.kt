package com.madruga665.bookmarks.ui.savemodal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.ui.collection.create.CreateCollectionBottomSheet
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.components.NeobrutalistSelectableFolderCard
import com.madruga665.bookmarks.ui.components.NeobrutalistTagInput
import com.madruga665.bookmarks.ui.savemodal.components.InlineCreateFolderForm
import com.madruga665.bookmarks.ui.savemodal.components.PinLinkToggleRow
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    if (!uiState.isVisible) return

    var isCreateCollectionOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeobrutalismTheme.colors.surface,
        dragHandle = {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .background(NeobrutalismTheme.colors.onSurface, RoundedCornerShape(3.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.testTag("tag_save_bookmark_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Title & Top-Right New Folder Icon Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.save_modal_title),
                    style = NeobrutalismTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = NeobrutalismTheme.colors.onSurface
                )

                // Top-Right New Folder Action Button
                NeobrutalistButton(
                    onClick = {
                        isCreateCollectionOpen = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("tag_modal_new_folder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CreateNewFolder,
                        contentDescription = stringResource(R.string.save_create_new_folder),
                        tint = NeobrutalismTheme.colors.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Target URL
            Text(
                text = uiState.targetUrl,
                style = NeobrutalismTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = NeobrutalismTheme.colors.subtext,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pin Link Toggle Row
            PinLinkToggleRow(
                isPinned = uiState.isPinned,
                onTogglePin = onTogglePin
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tag Input Section
            NeobrutalistTagInput(
                tags = uiState.tags,
                tagInput = uiState.tagInput,
                onTagInputChange = onTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                existingTags = uiState.existingTags,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Inline Create Folder Form (If active)
            if (uiState.isCreatingFolder) {
                InlineCreateFolderForm(
                    folderNameInput = uiState.newFolderNameInput,
                    onFolderNameChange = onNewFolderNameChange,
                    selectedColorAccent = uiState.newFolderColorAccent,
                    onColorAccentSelect = onNewFolderColorSelect,
                    onCreateFolderSubmit = onCreateFolderSubmit,
                    error = uiState.folderInputError
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Scrollable List of Available Folder Collections
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.availableCollections.forEach { collection ->
                    NeobrutalistSelectableFolderCard(
                        collection = collection,
                        isSelected = collection.id == uiState.selectedCollectionId,
                        onSelect = { onCollectionSelect(collection.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Main Action Button: Save to "[Collection Name]"
            val targetCollectionName = uiState.selectedCollection?.name ?: stringResource(R.string.save_unsorted)
            val buttonText = stringResource(R.string.save_to_collection_fmt, targetCollectionName)

            NeobrutalistButton(
                onClick = onConfirmSave,
                enabled = !uiState.isSaving,
                containerColor = NeobrutalismTheme.colors.accentYellow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("tag_confirm_save_bookmark_btn")
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("tag_save_bookmark_loading_spinner"),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = buttonText,
                        style = NeobrutalismTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (isCreateCollectionOpen) {
        CreateCollectionBottomSheet(
            onDismiss = { isCreateCollectionOpen = false },
            onCollectionCreated = { created ->
                onCollectionSelect(created.id)
                isCreateCollectionOpen = false
            },
            collectionRepository = collectionRepository
        )
    }
}
