package com.madruga665.bookmarks.ui.home

import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.data.repository.SyncRepository
import com.madruga665.bookmarks.data.repository.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelQuickSaveTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val collectionRepository: CollectionRepository = mockk(relaxed = true)
    private val bookmarkRepository: BookmarkRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val syncStatusFlow = MutableStateFlow(SyncStatus.IDLE)
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { collectionRepository.collections } returns flowOf(emptyList())
        coEvery { syncRepository.syncStatus } returns syncStatusFlow
        viewModel = HomeViewModel(collectionRepository, bookmarkRepository, syncRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onUrlInputChange_updatesQuickSaveInput() {
        viewModel.onUrlInputChange("https://kotlinlang.org")
        val state = viewModel.uiState.value as? HomeScreenUiState.Success
        assertNotNull(state)
        assertEquals("https://kotlinlang.org", state?.quickSaveUrlInput)
    }

    @Test
    fun onQuickSaveSubmit_invalidUrl_setsInputError() {
        viewModel.onUrlInputChange("not_a_valid_url")
        viewModel.onQuickSaveSubmit()
        val state = viewModel.uiState.value as? HomeScreenUiState.Success
        assertNotNull(state?.inputError)
    }

    @Test
    fun syncStatus_updatesHomeScreenUiState() {
        syncStatusFlow.value = SyncStatus.SYNCING
        val state = viewModel.uiState.value as? HomeScreenUiState.Success
        assertNotNull(state)
        assertEquals(SyncStatus.SYNCING, state?.syncStatus)

        syncStatusFlow.value = SyncStatus.SYNCED
        val stateUpdated = viewModel.uiState.value as? HomeScreenUiState.Success
        assertEquals(SyncStatus.SYNCED, stateUpdated?.syncStatus)
    }
}
