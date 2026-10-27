package com.dramflix.android.ui.detail

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val drama: DramaDetail? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class DetailViewModel(
    application: Application,
    private val dramaId: Int,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val scrollState = ScrollState(initial = 0)

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Also used by the Retry button, so it replaces any previous load instead of stacking collectors. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Observe cached detail from Room
            launch {
                dramaRepository.observeDetail(dramaId).collect { detail ->
                    if (detail != null) {
                        _state.update {
                            it.copy(drama = detail, loading = false)
                        }
                    }
                }
            }
            // Observe cached episodes from Room
            launch {
                dramaRepository.observeEpisodes(dramaId).collect { eps ->
                    if (eps.isNotEmpty()) {
                        _state.update {
                            it.copy(episodes = eps.sortedBy { ep -> ep.ep })
                        }
                    }
                }
            }
            // Network refresh — writes to Room, Flows auto-update
            launch {
                _state.update { it.copy(isRefreshing = true, error = null) }
                try {
                    // Detail and episodes are independent: fetch in parallel.
                    val detail = async { dramaRepository.refreshDetail(dramaId) }
                    val episodes = async { dramaRepository.refreshEpisodes(dramaId) }
                    detail.await()
                    episodes.await()
                    _state.update { it.copy(isRefreshing = false, error = null) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            loading = false,
                            isRefreshing = false,
                            error = if (it.drama == null) {
                                e.message ?: "Could not load drama"
                            } else null,
                        )
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        _state.update {
            val newFav = !it.isFavorite
            it.copy(
                isFavorite = newFav,
                snackbarMessage = if (newFav) "Added to your Watchlist!" else "Removed from Watchlist"
            )
        }
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

    companion object {
        fun factory(
            application: Application,
            dramaId: Int,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(application, dramaId) as T
                }
            }
    }
}
