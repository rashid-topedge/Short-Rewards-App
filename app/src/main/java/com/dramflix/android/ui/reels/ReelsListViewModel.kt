package com.dramflix.android.ui.reels

import android.app.Application
import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.ReelSort
import com.dramflix.android.data.repo.ReelsRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReelsListUiState(
    val query: String = "",
    val sort: String = "newest",
    val items: List<ReelItem> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = false,
    val likedReelIds: Set<Int> = emptySet(),
    val likeCounts: Map<Int, Int> = emptyMap(),
    val viewCounts: Map<Int, Int> = emptyMap(),
    val claimedRewardReelIds: Set<Int> = emptySet(),
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class ReelsListViewModel(
    application: Application,
    private val reelsRepository: ReelsRepository = ReelsRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ReelsListUiState(loading = true))
    val state: StateFlow<ReelsListUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    var currentPage: Int = 0

    private var observeJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        // Liked state is persisted locally, so hearts survive restarts.
        viewModelScope.launch {
            reelsRepository.observeLikedIds().collect { ids ->
                _state.update { it.copy(likedReelIds = ids) }
            }
        }
        observeAndRefresh()
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (_: Exception) {
                // keep existing behavior silent on first failure
            }
        }
    }

    private fun currentSort(): ReelSort = when (_state.value.sort) {
        "trending" -> ReelSort.Trending
        "random" -> ReelSort.Random
        else -> ReelSort.Newest
    }

    private fun observeAndRefresh() {
        val sort = currentSort()
        observeJob?.cancel()
        loadMoreJob?.cancel()
        // A cursor belongs to one feed; never reuse it for another sort.
        _state.update { it.copy(nextCursor = null, loadingMore = false) }
        observeJob = viewModelScope.launch {
            // Observe Room cache
            launch {
                reelsRepository.observeReels(sort).collect { reels ->
                    _state.update { it.copy(items = reels, loading = false) }
                }
            }
            // Network refresh
            launch {
                _state.update { it.copy(isRefreshing = true, error = null) }
                try {
                    val response = reelsRepository.refreshReels(sort = sort)
                    _state.update {
                        it.copy(
                            nextCursor = response.nextCursor,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            loading = false,
                            error = if (it.items.isEmpty()) {
                                e.message ?: "Could not load reels"
                            } else null,
                        )
                    }
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun setSort(sort: String) {
        currentPage = 0
        _state.update { it.copy(sort = sort, loading = true, items = emptyList()) }
        observeAndRefresh()
    }

    fun toggleMute() {
        _state.update { it.copy(isMuted = !it.isMuted) }
    }

    private val recordedViews = mutableSetOf<Int>()

    /**
     * Counts a view once per reel per session. The repository bumps the cached
     * count in Room (which re-emits [ReelsListUiState.items]), so no overlay is needed.
     */
    fun recordView(reelId: Int) {
        if (recordedViews.add(reelId)) {
            viewModelScope.launch {
                reelsRepository.recordView(reelId)
            }
        }
    }

    /**
     * Optimistic like: the repository flips the heart + count in Room right away
     * and rolls back if the server rejects it, so the UI always ends up matching
     * the server.
     */
    fun toggleLike(reelId: Int) {
        val willBeLiked = !_state.value.likedReelIds.contains(reelId)
        _state.update {
            it.copy(snackbarMessage = if (willBeLiked) "Liked!" else "Like removed")
        }
        viewModelScope.launch {
            try {
                val ok = reelsRepository.setLiked(reelId, willBeLiked)
                if (!ok) _state.update { it.copy(snackbarMessage = "Like action failed") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Like action failed") }
            }
        }
    }

    fun onReelFinished(reelId: Int) {
        if (_state.value.claimedRewardReelIds.contains(reelId)) return
        _state.update { it.copy(claimedRewardReelIds = it.claimedRewardReelIds + reelId) }
        viewModelScope.launch {
            try {
                val res = userRepository.rewardReel(reelId)
                _state.update { it.copy(snackbarMessage = res.message) }
            } catch (e: CancellationException) {
                _state.update { it.copy(claimedRewardReelIds = it.claimedRewardReelIds - reelId) }
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        claimedRewardReelIds = current.claimedRewardReelIds - reelId,
                        snackbarMessage = e.message ?: "Reward failed",
                    )
                }
            }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(query = "", sort = "newest", loading = true, items = emptyList()) }
        observeAndRefresh()
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
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Failed to claim ad reward") }
            }
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun refresh() {
        observeAndRefresh()
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (current.loadingMore || current.loading || current.isRefreshing) return
        val sort = currentSort()

        loadMoreJob = viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            try {
                val response = reelsRepository.refreshReels(
                    sort = sort,
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingMore = false,
                        error = e.message ?: "Could not load more reels",
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
                    return ReelsListViewModel(application) as T
                }
            }
    }
}
