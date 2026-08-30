package com.madruga665.bookmarks.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.ui.collection.create.CreateCollectionBottomSheet
import com.madruga665.bookmarks.ui.components.CollectionActionsOverlay
import com.madruga665.bookmarks.ui.components.CollectionOption
import com.madruga665.bookmarks.ui.components.DeleteCollectionDialog
import com.madruga665.bookmarks.ui.components.EditCollectionDialog
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.ui.home.components.HomeHeroHeadline
import com.madruga665.bookmarks.ui.home.components.HomeScreenTopBar
import com.madruga665.bookmarks.ui.home.components.MyCollectionsGrid
import com.madruga665.bookmarks.ui.home.components.QuickSaveBar
import com.madruga665.bookmarks.ui.savemodal.SaveBookmarkBottomSheet
import com.madruga665.bookmarks.ui.savemodal.SaveBookmarkViewModel
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistGridBackground

@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    saveBookmarkViewModel: SaveBookmarkViewModel,
    collectionRepository: CollectionRepository? = null,
    onUrlInputChange: (String) -> Unit,
    onPasteFromClipboard: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (CollectionEntity) -> Unit = {},
    onLongPressStart: (CollectionEntity, Offset, Offset, IntSize) -> Unit = { _, _, _, _ -> },
    onLongPressDrag: (Offset) -> Unit = {},
    onLongPressRelease: () -> Unit = {},
    onHoveredOptionChange: (CollectionOption?) -> Unit = {},
    onDismissActionsMenu: () -> Unit = {},
    onEditCollectionClick: (CollectionEntity) -> Unit = {},
    onShareCollectionClick: (CollectionEntity) -> Unit = {},
    onDeleteCollectionClick: (CollectionEntity) -> Unit = {},
    onDismissEditDialog: () -> Unit = {},
    onConfirmEditCollection: (id: String, name: String, colorAccent: String, iconKey: String) -> Unit = { _, _, _, _ -> },
    onDismissDeleteDialog: () -> Unit = {},
    onConfirmDeleteCollection: (collectionId: String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSyncSettings: () -> Unit = {},
    onNavigateToManageCollections: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val saveModalUiState by saveBookmarkViewModel.uiState.collectAsState()
    var isCreateCollectionOpen by remember { mutableStateOf(false) }

    val successState = uiState as? HomeScreenUiState.Success

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalismTheme.colors.background)
            .neobrutalistGridBackground(NeobrutalismTheme.colors.gridLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Action Bar
            HomeScreenTopBar(
                syncStatus = successState?.syncStatus ?: com.madruga665.bookmarks.data.repository.SyncStatus.IDLE,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSyncSettings = onNavigateToSyncSettings,
                onNavigateToManageCollections = {
                    isCreateCollectionOpen = true
                },
                onNavigateToSearch = onNavigateToSearch
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hero Headline Section
            HomeHeroHeadline()

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Save Bar Section
            when (uiState) {
                is HomeScreenUiState.Success -> {
                    QuickSaveBar(
                        urlValue = uiState.quickSaveUrlInput,
                        onUrlChange = onUrlInputChange,
                        onPasteClick = {
                            val text = clipboardManager.getText()?.text ?: ""
                            if (text.isNotBlank()) {
                                onPasteFromClipboard(text)
                            } else {
                                Toast.makeText(context, R.string.home_toast_clipboard_empty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddClick = {
                            if (uiState.quickSaveUrlInput.isNotBlank()) {
                                saveBookmarkViewModel.openSaveModal(uiState.quickSaveUrlInput)
                            } else {
                                Toast.makeText(context, R.string.home_toast_enter_valid_link, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    if (uiState.inputError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.inputError,
                            color = NeobrutalismTheme.colors.accentOrange,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Collections Grid Section
                    MyCollectionsGrid(
                        collections = uiState.collections,
                        onCollectionClick = onCollectionClick,
                        activeMenuCollection = uiState.activeMenuCollection,
                        touchPositionInWindow = uiState.touchPositionInWindow,
                        onHoveredOptionChange = onHoveredOptionChange,
                        onCollectionLongClick = onCollectionLongClick,
                        onLongPressStart = onLongPressStart,
                        onLongPressDrag = onLongPressDrag,
                        onLongPressRelease = onLongPressRelease
                    )
                }
                is HomeScreenUiState.Loading -> {
                    CircularProgressIndicator(
                        color = NeobrutalismTheme.colors.onSurface,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                is HomeScreenUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = NeobrutalismTheme.colors.accentOrange,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (successState != null) {
            CollectionActionsOverlay(
                collection = successState.activeMenuCollection,
                cardOffset = successState.activeCardOffset,
                cardSize = successState.activeCardSize,
                touchPositionInWindow = successState.touchPositionInWindow,
                dragPositionInWindow = successState.dragPositionInWindow,
                hoveredOption = successState.hoveredOption,
                onHoveredOptionChange = onHoveredOptionChange,
                onSelectItem = { option ->
                    val collection = successState.activeMenuCollection ?: return@CollectionActionsOverlay
                    when (option) {
                        CollectionOption.EDIT -> onEditCollectionClick(collection)
                        CollectionOption.SHARE -> onShareCollectionClick(collection)
                        CollectionOption.DELETE -> onDeleteCollectionClick(collection)
                    }
                },
                onDismiss = onDismissActionsMenu
            )

            // Edit Collection Dialog Modal
            EditCollectionDialog(
                collection = successState.collectionToEdit,
                onDismiss = onDismissEditDialog,
                onConfirmSave = onConfirmEditCollection
            )

            // Delete Collection Confirmation Dialog Modal
            DeleteCollectionDialog(
                collection = successState.collectionToDelete,
                onDismiss = onDismissDeleteDialog,
                onConfirmDelete = onConfirmDeleteCollection
            )
        }
    }

    // Save Bookmark Bottom Sheet Modal
    SaveBookmarkBottomSheet(
        uiState = saveModalUiState,
        collectionRepository = collectionRepository,
        onCollectionSelect = saveBookmarkViewModel::onSelectCollection,
        onTogglePin = saveBookmarkViewModel::onTogglePin,
        onToggleCreateFolder = saveBookmarkViewModel::onToggleCreateFolder,
        onNewFolderNameChange = saveBookmarkViewModel::onNewFolderNameChange,
        onNewFolderColorSelect = saveBookmarkViewModel::onNewFolderColorSelect,
        onCreateFolderSubmit = saveBookmarkViewModel::onCreateFolderSubmit,
        onTagInputChange = saveBookmarkViewModel::onTagInputChange,
        onAddTag = saveBookmarkViewModel::onAddTag,
        onRemoveTag = saveBookmarkViewModel::onRemoveTag,
        onConfirmSave = {
            saveBookmarkViewModel.onConfirmSave {
                onUrlInputChange("")
                Toast.makeText(context, R.string.save_bookmark_success, Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = saveBookmarkViewModel::dismissModal
    )

    // Create Collection Bottom Sheet Modal
    if (isCreateCollectionOpen) {
        CreateCollectionBottomSheet(
            onDismiss = { isCreateCollectionOpen = false },
            onCollectionCreated = { _ ->
                isCreateCollectionOpen = false
            },
            collectionRepository = collectionRepository
        )
    }
}
