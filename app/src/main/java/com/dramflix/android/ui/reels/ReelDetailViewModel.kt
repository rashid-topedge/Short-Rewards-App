package com.dramflix.android.ui.reels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.ReelsRepository
import com.dramflix.android.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

data class ReelDetailUiState(
    val reel: ReelItem? = null,
    val isLiked: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,

    val snackbarMessage: String? = null,
)

class ReelDetailViewModel(
    application: Application,
    private val reelId: Int,
    private val reelsRepository: ReelsRepository = ReelsRepository.getInstance(
        com.dramflix.android.data.api.DramaApi.getInstance(),
    ),
    private val userRepository: UserRepository = UserRepository.getInstance(
        com.dramflix.android.data.api.DramaApi.getInstance(),
        application,
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ReelDetailUiState())
    val state: StateFlow<ReelDetailUiState> = _state.asStateFlow()
    private var reelRewardClaimed = false

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val reel = reelsRepository.getReel(reelId)
                _state.update {
                    it.copy(
                        reel = reel,
                        isLiked = reel.likeCount > 0,
                        loading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Could not load reel",
                    )
                }
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            try {
                val ok = reelsRepository.likeReel(reelId)
                if (ok) {
                    _state.update { current ->
                        current.copy(
                            isLiked = true,
                            snackbarMessage = "Liked!",
                        )
                    }
                    val updated = reelsRepository.getReel(reelId)
                    _state.update { current ->
                        current.copy(
                            reel = updated,
                            snackbarMessage = if (current.isLiked) "Liked!" else "Like removed",
                        )
                    }
                }
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

    }

    }

        }
        load()
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
            reelsRepository: ReelsRepository = ReelsRepository.getInstance(
                com.dramflix.android.data.api.DramaApi.getInstance(),
            ),
            userRepository: UserRepository = UserRepository.getInstance(
                com.dramflix.android.data.api.DramaApi.getInstance(),
                application,
            ),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReelDetailViewModel(application, reelId, reelsRepository, userRepository) as T
                }
            }
    }
}

