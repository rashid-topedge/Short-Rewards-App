package com.dramflix.android.ui.reels

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.ReelsRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

data class ReelDetailUiState(
    val reel: ReelItem? = null,
    val isLiked: Boolean = false,
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class ReelDetailViewModel(
    application: Application,
    private val reelId: Int,
    private val reelsRepository: ReelsRepository = ReelsRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ReelDetailUiState())
    val state: StateFlow<ReelDetailUiState> = _state.asStateFlow()
    private var reelRewardClaimed = false

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val scrollState = ScrollState(initial = 0)
    var playbackPositionMs: Long = 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            // Observe cached reel from Room
            launch {
                reelsRepository.observeReel(reelId).collect { reel ->
                    if (reel != null) {
                        _state.update {
                            it.copy(
                                reel = reel,
                                isLiked = it.isLiked || reel.likeCount > 0,
                                loading = false,
                            )
                        }
                    }
                }
            }
            // Network refresh
            launch {
                _state.update { it.copy(isRefreshing = true, error = null) }
                try {
                    val reel = reelsRepository.getReel(reelId)
                    _state.update {
                        it.copy(
                            reel = reel,
                            isLiked = reel.likeCount > 0,
                            loading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                    recordView()
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            loading = false,
                            isRefreshing = false,
                            error = if (it.reel == null) {
                                e.message ?: "Could not load reel"
                            } else null,
                        )
                    }
                }
            }
        }
    }

    private var hasRecordedView = false
    private fun recordView() {
        if (!hasRecordedView) {
            hasRecordedView = true
            viewModelScope.launch {
                try {
                    reelsRepository.recordView(reelId)
                } catch (_: Exception) {}
            }
        }
    }

    fun toggleLike() {
        val willBeLiked = !_state.value.isLiked
        _state.update { current ->
            current.copy(
                isLiked = willBeLiked,
                snackbarMessage = if (willBeLiked) "Liked!" else "Like removed",
            )
        }
        viewModelScope.launch {
            try {
                reelsRepository.toggleLike(reelId, willBeLiked)
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Like failed") }
            }
        }
    }

    fun openRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = true) }
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (e: CancellationException) {
                throw e
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

    fun onReelFinished() {
        if (reelRewardClaimed) return
        reelRewardClaimed = true
        viewModelScope.launch {
            try {
                val res = userRepository.rewardReel(reelId)
                _state.update { it.copy(snackbarMessage = res.message) }
            } catch (e: Exception) {
                reelRewardClaimed = false
                _state.update { it.copy(snackbarMessage = e.message ?: "Reel reward failed") }
            }
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        fun factory(
            application: Application,
            reelId: Int,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReelDetailViewModel(application, reelId) as T
                }
            }
    }
}
