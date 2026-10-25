package com.dramflix.android.ui.catalog

import android.app.Application
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.model.DramaCard
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val draftQuery: String = "",
    val selectedGenre: String = "All",
    val items: List<DramaCard> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class CatalogViewModel(
    application: Application,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CatalogUiState(loading = true))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    /** Connectivity state — screens show an offline banner when false. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * LazyGridState lives in the ViewModel so scroll position survives
     * navigation (tab switches, back from detail) and config changes.
     */
    val gridState = LazyGridState()

    /** Active Room-observation job — cancelled & relaunched on query changes. */
    private var observeJob: Job? = null

    init {
        observeAndRefresh(query = "")
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (_: Exception) {
                // keep existing behavior silent on first failure
            }
        }
    }

    /**
     * Start observing the Room cache for [query] and kick off a network
     * refresh. Room Flow auto-pushes updates when the refresh writes back.
     */
    private fun observeAndRefresh(query: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            // Collect cached items from Room — emits immediately if cache exists
            launch {
                dramaRepository.observeDramas(query).collect { dramas ->
                    _state.update {
                        it.copy(
                            items = dramas,
                            loading = false,
                        )
                    }
                }
            }
            // Network refresh in parallel — writes to Room, Flow auto-updates
            launch {
                _state.update { it.copy(isRefreshing = true, error = null) }
                try {
                    val response = dramaRepository.refreshDramas(query = query)
                    _state.update {
                        it.copy(
                            nextCursor = response.nextCursor,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            loading = false,
                            error = if (it.items.isEmpty()) {
                                e.message ?: "Could not load dramas"
                            } else {
                                null // Don't show error if we have cached data
                            },
                        )
                    }
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draftQuery = value) }
    }

    fun submitSearch() {
        val q = _state.value.draftQuery.trim()
        _state.update { it.copy(query = q, draftQuery = q, loading = true, items = emptyList()) }
        observeAndRefresh(q)
    }

    fun clearSearch() {
        _state.update { it.copy(query = "", draftQuery = "", loading = true, items = emptyList()) }
        observeAndRefresh("")
    }

    fun selectGenre(genre: String) {
        val cleanGenre = genre.split(" ").firstOrNull() ?: genre
        val q = if (genre == "All") "" else cleanGenre
        _state.update {
            it.copy(
                selectedGenre = genre,
                query = q,
                draftQuery = if (genre == "All") "" else cleanGenre,
                loading = true,
                items = emptyList(),
            )
        }
        observeAndRefresh(q)
    }

    fun openRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = true) }
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (_: Exception) {
                // keep existing behavior silent on first failure
            }
        }
    }

    fun dismissRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = false) }
    }

    fun openRewardAdDialog() {
        _state.update { it.copy(showRewardAdDialog = true) }
    }

    fun dismissRewardAdDialog() {
        _state.update { it.copy(showRewardAdDialog = false) }
    }

    fun performCheckIn() {
        viewModelScope.launch {
            try {
                val res = userRepository.checkIn()
                _state.update { it.copy(snackbarMessage = res.message) }
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Failed to check in") }
            }
        }
    }

    fun onAdWatched() {
        viewModelScope.launch {
            try {
                val res = userRepository.rewardAd("REWARDED_VIDEO")
                _state.update { it.copy(snackbarMessage = res.message) }
                userRepository.sync()
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Failed to claim ad reward") }
            }
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun refresh() {
        val q = _state.value.query
        observeAndRefresh(q)
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loadingMore || _state.value.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            try {
                val response = dramaRepository.refreshDramas(
                    query = _state.value.query,
                    cursor = cursor,
                    append = true,
                )
                _state.update { current ->
                    current.copy(
                        nextCursor = response.nextCursor,
                        loadingMore = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingMore = false,
                        error = e.message ?: "Could not load more dramas",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CatalogViewModel(application) as T
                }
            }
    }
}
