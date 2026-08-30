package com.madruga665.bookmarks.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.ui.components.CollectionOption

sealed interface HomeScreenUiState {
    object Loading : HomeScreenUiState

    data class Success(
        val collections: List<CollectionEntity>,
        val quickSaveUrlInput: String = "",
        val inputError: String? = null,
        val isSaving: Boolean = false,
        val syncStatus: com.madruga665.bookmarks.data.repository.SyncStatus = com.madruga665.bookmarks.data.repository.SyncStatus.IDLE,
        val activeMenuCollection: CollectionEntity? = null,
        val activeCardOffset: Offset? = null,
        val activeCardSize: IntSize? = null,
        val collectionToEdit: CollectionEntity? = null,
        val collectionToDelete: CollectionEntity? = null,
        val touchPositionInWindow: Offset? = null,
        val dragPositionInWindow: Offset? = null,
        val hoveredOption: CollectionOption? = null
    ) : HomeScreenUiState

    data class Error(val message: String) : HomeScreenUiState
}
