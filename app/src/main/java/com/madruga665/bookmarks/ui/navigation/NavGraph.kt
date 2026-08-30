package com.madruga665.bookmarks.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.local.BookmarkEntity
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.data.repository.SettingsRepository
import com.madruga665.bookmarks.ui.bookmark.BookmarkDetailScreen
import com.madruga665.bookmarks.ui.bookmark.BookmarkDetailViewModel
import com.madruga665.bookmarks.ui.components.BookmarkOption
import com.madruga665.bookmarks.ui.utils.LinkOpener
import com.madruga665.bookmarks.ui.utils.ShareUtils
import com.madruga665.bookmarks.ui.utils.UrlUtils
import com.madruga665.bookmarks.ui.collection.CollectionDetailScreen
import com.madruga665.bookmarks.ui.collection.CollectionDetailViewModel
import com.madruga665.bookmarks.ui.home.HomeScreen
import com.madruga665.bookmarks.ui.home.HomeViewModel
import com.madruga665.bookmarks.ui.savemodal.SaveBookmarkBottomSheet
import com.madruga665.bookmarks.ui.savemodal.SaveBookmarkViewModel
import com.madruga665.bookmarks.ui.search.SearchScreen
import com.madruga665.bookmarks.ui.search.SearchViewModel
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.repository.SyncRepository
import com.madruga665.bookmarks.ui.settings.sync.SyncSettingsScreen
import com.madruga665.bookmarks.ui.settings.sync.SyncSettingsViewModel
import com.madruga665.bookmarks.ui.settings.SettingsScreen
import com.madruga665.bookmarks.ui.settings.SettingsViewModel
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import androidx.lifecycle.SavedStateHandle

object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SYNC_SETTINGS = "sync_settings"
    const val MANAGE_COLLECTIONS = "manage_collections"
    const val FOLDER_DETAIL = "folder_detail/{folderId}"
    const val BOOKMARK_DETAIL = "bookmark_detail/{bookmarkId}"

    fun folderDetail(folderId: String) = "folder_detail/$folderId"
    fun bookmarkDetail(bookmarkId: String) = "bookmark_detail/$bookmarkId"
}

@Composable
fun BookmarksNavGraph(
    homeViewModel: HomeViewModel,
    saveBookmarkViewModel: SaveBookmarkViewModel,
    collectionRepository: CollectionRepository,
    bookmarkRepository: BookmarkRepository,
    settingsRepository: SettingsRepository,
    syncRepository: SyncRepository? = null,
    peerDiscoveryManager: PeerDiscoveryManager? = null,
    navController: NavHostController = rememberNavController()
) {
    val uiState by homeViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            val context = LocalContext.current
            HomeScreen(
                uiState = uiState,
                saveBookmarkViewModel = saveBookmarkViewModel,
                collectionRepository = collectionRepository,
                onUrlInputChange = homeViewModel::onUrlInputChange,
                onPasteFromClipboard = homeViewModel::onPasteFromClipboard,
                onCollectionClick = { collectionId: String ->
                    navController.navigate(NavRoutes.folderDetail(collectionId))
                },
                onCollectionLongClick = homeViewModel::openActionsMenu,
                onLongPressStart = homeViewModel::onLongPressStart,
                onLongPressDrag = homeViewModel::onLongPressDrag,
                onLongPressRelease = {
                    homeViewModel.onLongPressRelease { collection ->
                        homeViewModel.shareCollection(context, collection)
                    }
                },
                onHoveredOptionChange = homeViewModel::onHoveredOptionChange,
                onDismissActionsMenu = homeViewModel::dismissActionsMenu,
                onEditCollectionClick = homeViewModel::openEditDialog,
                onShareCollectionClick = { collection: CollectionEntity ->
                    homeViewModel.shareCollection(context, collection)
                },
                onDeleteCollectionClick = homeViewModel::openDeleteDialog,
                onDismissEditDialog = homeViewModel::dismissEditDialog,
                onConfirmEditCollection = homeViewModel::updateCollection,
                onDismissDeleteDialog = homeViewModel::dismissDeleteDialog,
                onConfirmDeleteCollection = homeViewModel::deleteCollection,
                onNavigateToSearch = {
                    navController.navigate(NavRoutes.SEARCH)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToSyncSettings = {
                    navController.navigate(NavRoutes.SYNC_SETTINGS)
                },
                onNavigateToManageCollections = {
                    navController.navigate(NavRoutes.MANAGE_COLLECTIONS)
                }
            )
        }

        composable(NavRoutes.SEARCH) {
            val context = LocalContext.current
            val searchViewModel = remember {
                SearchViewModel(
                    bookmarkRepository = bookmarkRepository,
                    collectionRepository = collectionRepository
                )
            }
            val searchUiState by searchViewModel.uiState.collectAsState()

            SearchScreen(
                uiState = searchUiState,
                onQueryChange = searchViewModel::onQueryChange,
                onClearQuery = searchViewModel::onClearQuery,
                onCancelClick = { navController.popBackStack() },
                onBookmarkClick = { bookmarkId: String ->
                    navController.navigate(NavRoutes.bookmarkDetail(bookmarkId))
                },
                onToggleTagFilter = searchViewModel::onToggleTagFilter,
                onClearTagFilters = searchViewModel::onClearTagFilters,
                onBookmarkLongPressStart = searchViewModel::onLongPressStart,
                onBookmarkLongPressDrag = searchViewModel::onLongPressDrag,
                onBookmarkLongPressRelease = {
                    searchViewModel.onLongPressRelease { bookmark, option ->
                        when (option) {
                            BookmarkOption.OPEN -> LinkOpener.openUrl(context, bookmark.url)
                            BookmarkOption.PIN -> searchViewModel.togglePin(bookmark.id)
                            BookmarkOption.SHARE -> ShareUtils.shareBookmark(context, bookmark)
                            BookmarkOption.DELETE -> searchViewModel.openDeleteDialog(bookmark)
                        }
                    }
                },
                onBookmarkHoveredOptionChange = searchViewModel::onHoveredOptionChange,
                onDismissBookmarkActionsMenu = searchViewModel::dismissActionsMenu,
                onDismissDeleteBookmarkDialog = searchViewModel::dismissDeleteDialog,
                onConfirmDeleteBookmark = searchViewModel::deleteBookmark,
                onBookmarkSelectAction = { bookmark, option ->
                    when (option) {
                        BookmarkOption.OPEN -> LinkOpener.openUrl(context, bookmark.url)
                        BookmarkOption.PIN -> searchViewModel.togglePin(bookmark.id)
                        BookmarkOption.SHARE -> ShareUtils.shareBookmark(context, bookmark)
                        BookmarkOption.DELETE -> searchViewModel.openDeleteDialog(bookmark)
                    }
                    searchViewModel.dismissActionsMenu()
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            val context = LocalContext.current
            val settingsViewModel = remember {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    bookmarkRepository = bookmarkRepository,
                    collectionRepository = collectionRepository
                )
            }
            val settingsUiState by settingsViewModel.uiState.collectAsState()

            SettingsScreen(
                uiState = settingsUiState,
                onBackClick = { navController.popBackStack() },
                onThemeSelect = settingsViewModel::setThemeMode,
                onLanguageSelect = settingsViewModel::setLanguage,
                onToggleHapticFeedback = settingsViewModel::toggleHapticFeedback,
                onNavigateToSyncSettings = {
                    navController.navigate(NavRoutes.SYNC_SETTINGS)
                },
                onExportBackupClick = {
                    Toast.makeText(context, R.string.toast_exporting_backup, Toast.LENGTH_SHORT).show()
                },
                onRestoreBackupClick = {
                    Toast.makeText(context, R.string.toast_restore_backup_soon, Toast.LENGTH_SHORT).show()
                },
                onImportBookmarksClick = {
                    Toast.makeText(context, R.string.toast_import_bookmarks_soon, Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(NavRoutes.SYNC_SETTINGS) {
            val syncViewModel: SyncSettingsViewModel = if (syncRepository != null && peerDiscoveryManager != null) {
                remember {
                    SyncSettingsViewModel(
                        syncRepository = syncRepository,
                        peerDiscoveryManager = peerDiscoveryManager
                    )
                }
            } else {
                androidx.hilt.navigation.compose.hiltViewModel()
            }
            val syncUiState by syncViewModel.uiState.collectAsState()

            SyncSettingsScreen(
                uiState = syncUiState,
                onBackClick = { navController.popBackStack() },
                onInitiatePairing = syncViewModel::initiatePairing,
                onVerificationCodeChange = syncViewModel::onVerificationCodeChange,
                onConfirmPairing = { syncViewModel.confirmPairing() },
                onDismissPairingDialog = syncViewModel::dismissPairingDialog,
                onSyncNow = syncViewModel::triggerManualSync,
                onSyncDevice = syncViewModel::syncWithDevice,
                onUnpairDevice = syncViewModel::unpairDevice,
                onRefreshDiscovery = syncViewModel::startDiscovery,
                onManualHostChange = syncViewModel::onManualHostChange,
                onManualPortChange = syncViewModel::onManualPortChange,
                onPairManualHost = { syncViewModel.pairWithManualHost() }
            )
        }


        composable(NavRoutes.MANAGE_COLLECTIONS) {
            PlaceholderDestination(title = "Manage Collections Screen")
        }

        composable(NavRoutes.FOLDER_DETAIL) { backStackEntry ->
            val context = LocalContext.current
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            val viewModel = remember(folderId) {
                CollectionDetailViewModel(
                    collectionId = folderId,
                    collectionRepository = collectionRepository,
                    bookmarkRepository = bookmarkRepository
                )
            }
            val detailUiState by viewModel.uiState.collectAsState()
            val saveModalUiState by saveBookmarkViewModel.uiState.collectAsState()

            CollectionDetailScreen(
                uiState = detailUiState,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddLinkClick = {
                    saveBookmarkViewModel.openSaveModal("https://")
                    saveBookmarkViewModel.onSelectCollection(folderId)
                },
                onOptionsClick = {
                    // Options click handler
                },
                onBookmarkClick = { bookmark: BookmarkEntity ->
                    navController.navigate(NavRoutes.bookmarkDetail(bookmark.id))
                },
                onSubcollectionClick = { subcollectionId: String ->
                    navController.navigate(NavRoutes.folderDetail(subcollectionId))
                },
                onBookmarkLongPressStart = viewModel::onLongPressStart,
                onBookmarkLongPressDrag = viewModel::onLongPressDrag,
                onBookmarkLongPressRelease = {
                    viewModel.onLongPressRelease { bookmark, option ->
                        when (option) {
                            BookmarkOption.OPEN -> LinkOpener.openUrl(context, bookmark.url)
                            BookmarkOption.PIN -> viewModel.togglePin(bookmark.id)
                            BookmarkOption.SHARE -> ShareUtils.shareBookmark(context, bookmark)
                            BookmarkOption.DELETE -> viewModel.openDeleteDialog(bookmark)
                        }
                    }
                },
                onBookmarkHoveredOptionChange = viewModel::onHoveredOptionChange,
                onDismissBookmarkActionsMenu = viewModel::dismissActionsMenu,
                onDismissDeleteBookmarkDialog = viewModel::dismissDeleteDialog,
                onConfirmDeleteBookmark = viewModel::deleteBookmark,
                onBookmarkSelectAction = { bookmark, option ->
                    when (option) {
                        BookmarkOption.OPEN -> LinkOpener.openUrl(context, bookmark.url)
                        BookmarkOption.PIN -> viewModel.togglePin(bookmark.id)
                        BookmarkOption.SHARE -> ShareUtils.shareBookmark(context, bookmark)
                        BookmarkOption.DELETE -> viewModel.openDeleteDialog(bookmark)
                    }
                    viewModel.dismissActionsMenu()
                }
            )

            SaveBookmarkBottomSheet(
                uiState = saveModalUiState,
                collectionRepository = collectionRepository,
                onCollectionSelect = saveBookmarkViewModel::onSelectCollection,
                onTogglePin = saveBookmarkViewModel::onTogglePin,
                onToggleCreateFolder = saveBookmarkViewModel::onToggleCreateFolder,
                onNewFolderNameChange = saveBookmarkViewModel::onNewFolderNameChange,
                onNewFolderColorSelect = saveBookmarkViewModel::onNewFolderColorSelect,
                onCreateFolderSubmit = saveBookmarkViewModel::onCreateFolderSubmit,
                onConfirmSave = {
                    saveBookmarkViewModel.onConfirmSave {
                        Toast.makeText(context, R.string.save_bookmark_success, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = saveBookmarkViewModel::dismissModal
            )
        }

        composable(NavRoutes.BOOKMARK_DETAIL) { backStackEntry ->
            val context = LocalContext.current
            val bookmarkId = backStackEntry.arguments?.getString("bookmarkId") ?: ""
            val bookmarkDetailViewModel = remember(bookmarkId) {
                BookmarkDetailViewModel(
                    bookmarkRepository = bookmarkRepository,
                    collectionRepository = collectionRepository,
                    savedStateHandle = SavedStateHandle(mapOf("bookmarkId" to bookmarkId))
                )
            }
            val bookmarkUiState by bookmarkDetailViewModel.uiState.collectAsState()

            BookmarkDetailScreen(
                uiState = bookmarkUiState,
                onBackClick = { navController.popBackStack() },
                onRefreshClick = bookmarkDetailViewModel::onRefreshMetadata,
                onShareClick = {
                    bookmarkUiState.bookmark?.let { bm ->
                        ShareUtils.shareBookmark(context, bm)
                    }
                },
                onMoveClick = bookmarkDetailViewModel::onOpenMoveCollectionSheet,
                onDeleteClick = bookmarkDetailViewModel::onOpenDeleteDialog,
                onTogglePin = bookmarkDetailViewModel::onTogglePin,
                onStartEditingTitle = bookmarkDetailViewModel::onStartEditingTitle,
                onTitleChange = bookmarkDetailViewModel::onTitleChange,
                onSaveTitle = bookmarkDetailViewModel::onSaveTitle,
                onCancelEditingTitle = bookmarkDetailViewModel::onCancelEditingTitle,
                onUrlClick = { url: String -> UrlUtils.openBrowserUrl(context, url) },
                onToggleDescriptionExpanded = bookmarkDetailViewModel::onToggleDescriptionExpanded,
                onStartEditingNotes = bookmarkDetailViewModel::onStartEditingNotes,
                onNotesChange = bookmarkDetailViewModel::onNotesChange,
                onSaveNotes = bookmarkDetailViewModel::onSaveNotes,
                onCancelEditingNotes = bookmarkDetailViewModel::onCancelEditingNotes,
                onOpenAddTagDialog = bookmarkDetailViewModel::onOpenAddTagDialog,
                onNewTagInputChange = bookmarkDetailViewModel::onNewTagInputChange,
                onSaveNewTag = bookmarkDetailViewModel::onSaveNewTag,
                onDismissAddTagDialog = bookmarkDetailViewModel::onDismissAddTagDialog,
                onRemoveTag = bookmarkDetailViewModel::onRemoveTag,
                onOpenMoveCollectionSheet = bookmarkDetailViewModel::onOpenMoveCollectionSheet,
                onDismissMoveCollectionSheet = bookmarkDetailViewModel::onDismissMoveCollectionSheet,
                onSelectCollection = bookmarkDetailViewModel::onSelectCollection,
                onOpenDeleteDialog = bookmarkDetailViewModel::onOpenDeleteDialog,
                onDismissDeleteDialog = bookmarkDetailViewModel::onDismissDeleteDialog,
                onConfirmDelete = {
                    bookmarkDetailViewModel.onConfirmDelete {
                        navController.popBackStack()
                    }
                },
                onClearUserMessage = bookmarkDetailViewModel::clearUserMessage
            )
        }
    }
}

@Composable
private fun PlaceholderDestination(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeobrutalismTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = NeobrutalismTheme.typography.headlineMedium,
            color = NeobrutalismTheme.colors.onSurface
        )
    }
}
