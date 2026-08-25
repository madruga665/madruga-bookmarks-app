package com.madruga665.bookmarks.ui.savemodal

import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveBookmarkViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val collectionRepository: CollectionRepository = mockk(relaxed = true)
    private val bookmarkRepository: BookmarkRepository = mockk(relaxed = true)
    private lateinit var viewModel: SaveBookmarkViewModel

    private val sampleCollections = listOf(
        CollectionEntity("col_unsorted", "Unsorted", 0, 0, null, "folder", "YELLOW", 0, 0),
        CollectionEntity("col_ia", "IA", 2, 0, null, "code", "YELLOW", 0, 0)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { collectionRepository.collections } returns flowOf(sampleCollections)
        viewModel = SaveBookmarkViewModel(collectionRepository, bookmarkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectionsLoadedFromRepository_updatesAvailableCollections() {
        val state = viewModel.uiState.value
        assertEquals(2, state.availableCollections.size)
        assertEquals("col_unsorted", state.availableCollections[0].id)
    }

    @Test
    fun openSaveModal_validUrl_showsModalAndInitializesState() {
        viewModel.openSaveModal("  https://github.com/catppuccin/nvim  ")
        val state = viewModel.uiState.value
        assertTrue(state.isVisible)
        assertEquals("https://github.com/catppuccin/nvim", state.targetUrl)
        assertEquals("col_unsorted", state.selectedCollectionId)
        assertFalse(state.isPinned)
        assertFalse(state.isCreatingFolder)
        assertEquals("", state.newFolderNameInput)
        assertEquals("YELLOW", state.newFolderColorAccent)
        assertNull(state.folderInputError)
        assertNull(state.error)
        assertFalse(state.isSaving)
    }

    @Test
    fun openSaveModal_blankUrl_doesNotOpenModal() {
        viewModel.openSaveModal("   ")
        val state = viewModel.uiState.value
        assertFalse(state.isVisible)
        assertEquals("", state.targetUrl)
    }

    @Test
    fun dismissModal_setsIsVisibleToFalse() {
        viewModel.openSaveModal("https://kotlinlang.org")
        assertTrue(viewModel.uiState.value.isVisible)

        viewModel.dismissModal()
        assertFalse(viewModel.uiState.value.isVisible)
    }

    @Test
    fun onSelectCollection_updatesSelectedCollectionId() {
        viewModel.openSaveModal("https://github.com/catppuccin/nvim")
        viewModel.onSelectCollection("col_ia")
        val state = viewModel.uiState.value
        assertEquals("col_ia", state.selectedCollectionId)
        assertEquals("Save to \"IA\"", state.saveButtonText)
    }

    @Test
    fun onTogglePin_togglesIsPinnedState() {
        viewModel.openSaveModal("https://github.com/catppuccin/nvim")
        assertFalse(viewModel.uiState.value.isPinned)

        viewModel.onTogglePin()
        assertTrue(viewModel.uiState.value.isPinned)

        viewModel.onTogglePin()
        assertFalse(viewModel.uiState.value.isPinned)
    }

    @Test
    fun onToggleCreateFolder_togglesIsCreatingFolderAndResetsInput() {
        viewModel.onNewFolderNameChange("Draft")
        viewModel.onToggleCreateFolder()

        val stateOpen = viewModel.uiState.value
        assertTrue(stateOpen.isCreatingFolder)
        assertEquals("", stateOpen.newFolderNameInput)
        assertEquals("YELLOW", stateOpen.newFolderColorAccent)
        assertNull(stateOpen.folderInputError)

        viewModel.onToggleCreateFolder()
        assertFalse(viewModel.uiState.value.isCreatingFolder)
    }

    @Test
    fun onNewFolderNameChange_updatesInputAndClearsError() {
        viewModel.onNewFolderNameChange("")
        viewModel.onCreateFolderSubmit()
        assertEquals("Folder name cannot be empty", viewModel.uiState.value.folderInputError)

        viewModel.onNewFolderNameChange("Design")
        val state = viewModel.uiState.value
        assertEquals("Design", state.newFolderNameInput)
        assertNull(state.folderInputError)
    }

    @Test
    fun onNewFolderColorSelect_updatesColorAccent() {
        viewModel.onNewFolderColorSelect("PURPLE")
        assertEquals("PURPLE", viewModel.uiState.value.newFolderColorAccent)
    }

    @Test
    fun onCreateFolderSubmit_blankName_setsValidationError() {
        viewModel.onToggleCreateFolder()
        viewModel.onNewFolderNameChange("   ")
        viewModel.onCreateFolderSubmit()

        val state = viewModel.uiState.value
        assertEquals("Folder name cannot be empty", state.folderInputError)
    }

    @Test
    fun onCreateFolderSubmit_validName_createsCollectionAndSelectsIt() {
        val newCollection = CollectionEntity(
            id = "col_design",
            name = "Design",
            linkCount = 0,
            subcollectionCount = 0,
            parentId = null,
            iconKey = "folder",
            colorAccent = "PURPLE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        coEvery { collectionRepository.createCollection("Design", "PURPLE") } returns newCollection

        viewModel.onToggleCreateFolder()
        viewModel.onNewFolderNameChange("Design")
        viewModel.onNewFolderColorSelect("PURPLE")
        viewModel.onCreateFolderSubmit()

        val state = viewModel.uiState.value
        assertEquals("col_design", state.selectedCollectionId)
        assertFalse(state.isCreatingFolder)
        assertEquals("", state.newFolderNameInput)
        assertNull(state.folderInputError)

        coVerify { collectionRepository.createCollection("Design", "PURPLE") }
    }

    @Test
    fun onCreateFolderSubmit_repositoryReturnsNull_setsFolderInputError() {
        coEvery { collectionRepository.createCollection("Design", "PURPLE") } returns null

        viewModel.onToggleCreateFolder()
        viewModel.onNewFolderNameChange("Design")
        viewModel.onNewFolderColorSelect("PURPLE")
        viewModel.onCreateFolderSubmit()

        val state = viewModel.uiState.value
        assertEquals("Failed to create folder", state.folderInputError)
        assertTrue(state.isCreatingFolder)
    }

    @Test
    fun onConfirmSave_success_savesBookmarkAndDismissesModal() {
        coEvery {
            bookmarkRepository.quickSaveBookmark(
                url = "https://kotlinlang.org",
                collectionId = "col_ia",
                isPinned = true,
                tags = any()
            )
        } returns true

        viewModel.openSaveModal("https://kotlinlang.org")
        viewModel.onSelectCollection("col_ia")
        viewModel.onTogglePin()

        var callbackCalled = false
        viewModel.onConfirmSave {
            callbackCalled = true
        }

        val state = viewModel.uiState.value
        assertFalse(state.isVisible)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertTrue(callbackCalled)

        coVerify {
            bookmarkRepository.quickSaveBookmark(
                url = "https://kotlinlang.org",
                collectionId = "col_ia",
                isPinned = true,
                tags = any()
            )
        }
    }

    @Test
    fun onConfirmSave_failure_setsErrorStateAndKeepsModalVisible() {
        coEvery {
            bookmarkRepository.quickSaveBookmark(
                url = "https://kotlinlang.org",
                collectionId = "col_unsorted",
                isPinned = false,
                tags = any()
            )
        } returns false

        viewModel.openSaveModal("https://kotlinlang.org")

        var callbackCalled = false
        viewModel.onConfirmSave {
            callbackCalled = true
        }

        val state = viewModel.uiState.value
        assertTrue(state.isVisible)
        assertFalse(state.isSaving)
        assertEquals("Failed to save bookmark", state.error)
        assertFalse(callbackCalled)
    }

    @Test
    fun onConfirmSave_blankUrl_doesNotTriggerSave() {
        viewModel.onConfirmSave {}
        coVerify(exactly = 0) { bookmarkRepository.quickSaveBookmark(any(), any(), any(), any()) }
    }

    @Test
    fun onConfirmSave_setsIsSavingTrueDuringExecution() = runTest {
        val deferred = CompletableDeferred<Boolean>()
        coEvery {
            bookmarkRepository.quickSaveBookmark(
                url = "https://kotlinlang.org",
                collectionId = any(),
                isPinned = any(),
                tags = any()
            )
        } coAnswers {
            deferred.await()
        }

        viewModel.openSaveModal("https://kotlinlang.org")

        var callbackCalled = false
        viewModel.onConfirmSave {
            callbackCalled = true
        }

        // Verify isSaving is true and modal is still visible while in-flight
        assertTrue(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.isVisible)
        assertFalse(callbackCalled)

        // Complete the in-flight save operation
        deferred.complete(true)
        testScheduler.advanceUntilIdle()

        // Verify state transitions to isSaving = false and isVisible = false on completion
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.isVisible)
        assertNull(viewModel.uiState.value.error)
        assertTrue(callbackCalled)
    }

    @Test
    fun onConfirmSave_whileSaving_ignoresDuplicateInvocation() = runTest {
        val deferred = CompletableDeferred<Boolean>()
        coEvery {
            bookmarkRepository.quickSaveBookmark(
                url = "https://kotlinlang.org",
                collectionId = any(),
                isPinned = any(),
                tags = any()
            )
        } coAnswers {
            deferred.await()
        }

        viewModel.openSaveModal("https://kotlinlang.org")

        var firstCallbackCalled = false
        var secondCallbackCalled = false

        viewModel.onConfirmSave { firstCallbackCalled = true }
        assertTrue(viewModel.uiState.value.isSaving)

        // Duplicate invocation while saving is in progress
        viewModel.onConfirmSave { secondCallbackCalled = true }

        // Only one save call should be dispatched
        coVerify(exactly = 1) {
            bookmarkRepository.quickSaveBookmark(any(), any(), any(), any())
        }

        deferred.complete(true)
        testScheduler.advanceUntilIdle()

        assertTrue(firstCallbackCalled)
        assertFalse(secondCallbackCalled)
    }

    // ---- Tag-related tests (T008) ----

    @Test
    fun onAddTag_validTag_addsToTagsAndClearsInput() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onTagInputChange("android")
        viewModel.onAddTag("android")

        val state = viewModel.uiState.value
        assertEquals(listOf("android"), state.tags)
        assertEquals("", state.tagInput)
    }

    @Test
    fun onAddTag_duplicateTag_isIgnored() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("kotlin")
        viewModel.onAddTag("Kotlin")

        assertEquals(listOf("kotlin"), viewModel.uiState.value.tags)
    }

    @Test
    fun onAddTag_withHashPrefix_normalizesTag() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("#Android")

        assertEquals(listOf("android"), viewModel.uiState.value.tags)
    }

    @Test
    fun onAddTag_blankTag_isIgnored() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("   ")

        assertTrue(viewModel.uiState.value.tags.isEmpty())
    }

    @Test
    fun onAddTag_atMaxLimit_isRejected() {
        viewModel.openSaveModal("https://example.com")
        repeat(10) { i -> viewModel.onAddTag("tag$i") }
        assertEquals(10, viewModel.uiState.value.tags.size)

        viewModel.onAddTag("tag10")
        assertEquals(10, viewModel.uiState.value.tags.size)
    }

    @Test
    fun onRemoveTag_removesMatchingTag() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("kotlin")
        viewModel.onAddTag("compose")
        viewModel.onRemoveTag("kotlin")

        assertEquals(listOf("compose"), viewModel.uiState.value.tags)
    }

    @Test
    fun onRemoveTag_caseInsensitive() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("kotlin")
        viewModel.onRemoveTag("Kotlin")

        assertTrue(viewModel.uiState.value.tags.isEmpty())
    }

    @Test
    fun onTagInputChange_truncatesAt25Characters() {
        viewModel.openSaveModal("https://example.com")
        val longInput = "a".repeat(30)
        viewModel.onTagInputChange(longInput)

        assertEquals(25, viewModel.uiState.value.tagInput.length)
    }

    @Test
    fun onConfirmSave_withTags_passesTagsToRepository() {
        coEvery {
            bookmarkRepository.quickSaveBookmark(
                url = any(),
                collectionId = any(),
                isPinned = any(),
                tags = any()
            )
        } returns true

        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("kotlin")
        viewModel.onAddTag("compose")

        viewModel.onConfirmSave {}

        coVerify {
            bookmarkRepository.quickSaveBookmark(
                url = "https://example.com",
                collectionId = any(),
                isPinned = false,
                tags = "kotlin,compose"
            )
        }
    }

    @Test
    fun openSaveModal_resetsTags() {
        viewModel.openSaveModal("https://example.com")
        viewModel.onAddTag("kotlin")
        assertEquals(1, viewModel.uiState.value.tags.size)

        viewModel.openSaveModal("https://example2.com")
        assertTrue(viewModel.uiState.value.tags.isEmpty())
        assertEquals("", viewModel.uiState.value.tagInput)
    }
}
