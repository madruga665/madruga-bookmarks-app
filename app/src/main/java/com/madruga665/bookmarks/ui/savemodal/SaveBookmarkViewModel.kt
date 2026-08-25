package com.madruga665.bookmarks.ui.savemodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.ui.utils.tagList
import com.madruga665.bookmarks.ui.utils.toTagString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaveBookmarkViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaveBookmarkModalUiState())
    val uiState: StateFlow<SaveBookmarkModalUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
        loadExistingTags()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            collectionRepository.collections.collectLatest { collections ->
                _uiState.update {
                    it.copy(availableCollections = collections)
                }
            }
        }
    }

    private fun loadExistingTags() {
        viewModelScope.launch {
            bookmarkRepository.allBookmarks.collectLatest { bookmarks ->
                val allTags = bookmarks.flatMap { it.tagList }.distinct().sorted()
                _uiState.update {
                    it.copy(existingTags = allTags)
                }
            }
        }
    }

    fun openSaveModal(targetUrl: String) {
        if (targetUrl.isNotBlank()) {
            val collections = _uiState.value.availableCollections
            _uiState.update {
                it.copy(
                    isVisible = true,
                    targetUrl = targetUrl.trim(),
                    selectedCollectionId = collections.firstOrNull()?.id ?: "col_unsorted",
                    isPinned = false,
                    isCreatingFolder = false,
                    newFolderNameInput = "",
                    newFolderColorAccent = "YELLOW",
                    folderInputError = null,
                    error = null,
                    isSaving = false,
                    tags = emptyList(),
                    tagInput = ""
                )
            }
        }
    }

    fun dismissModal() {
        _uiState.update {
            it.copy(isVisible = false)
        }
    }

    fun onSelectCollection(collectionId: String) {
        _uiState.update {
            it.copy(selectedCollectionId = collectionId)
        }
    }

    fun onTogglePin() {
        _uiState.update {
            it.copy(isPinned = !it.isPinned)
        }
    }

    fun onToggleCreateFolder() {
        _uiState.update {
            it.copy(
                isCreatingFolder = !it.isCreatingFolder,
                newFolderNameInput = "",
                newFolderColorAccent = "YELLOW",
                folderInputError = null
            )
        }
    }

    fun onNewFolderNameChange(input: String) {
        _uiState.update {
            it.copy(
                newFolderNameInput = input,
                folderInputError = null
            )
        }
    }

    fun onNewFolderColorSelect(color: String) {
        _uiState.update {
            it.copy(newFolderColorAccent = color)
        }
    }

    fun onTagInputChange(input: String) {
        _uiState.update { it.copy(tagInput = input.take(25)) }
    }

    fun onAddTag(tag: String) {
        val clean = tag.trim().removePrefix("#").lowercase()
        if (clean.isBlank() || _uiState.value.tags.size >= 10 || _uiState.value.tags.contains(clean)) {
            return
        }
        _uiState.update { it.copy(tags = it.tags + clean, tagInput = "") }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags.filterNot { t -> t.equals(tag, ignoreCase = true) }) }
    }

    fun onCreateFolderSubmit() {
        val name = _uiState.value.newFolderNameInput.trim()
        if (name.isBlank()) {
            _uiState.update {
                it.copy(folderInputError = "Folder name cannot be empty")
            }
            return
        }

        viewModelScope.launch {
            val created = collectionRepository.createCollection(
                name = name,
                colorAccent = _uiState.value.newFolderColorAccent
            )
            if (created != null) {
                _uiState.update {
                    it.copy(
                        selectedCollectionId = created.id,
                        isCreatingFolder = false,
                        newFolderNameInput = "",
                        folderInputError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(folderInputError = "Failed to create folder")
                }
            }
        }
    }

    fun onConfirmSave(onSuccess: () -> Unit) {
        val current = _uiState.value
        if (current.isSaving || current.targetUrl.isBlank()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = bookmarkRepository.quickSaveBookmark(
                url = current.targetUrl,
                collectionId = current.selectedCollectionId,
                isPinned = current.isPinned,
                tags = current.tags.toTagString()
            )
            if (success) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isVisible = false
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save bookmark"
                    )
                }
            }
        }
    }
}
