package com.dramflix.android.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.api.DramaApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val drama: DramaDetail? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class DetailViewModel(
    application: Application,
    private val dramaId: Int,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(
        DramaApi.getInstance(),
    ),
    private val userRepository: UserRepository = UserRepository.getInstance(
        DramaApi.getInstance(),
        application,
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val dramaDeferred = async { dramaRepository.getDrama(dramaId) }
                val episodesDeferred = async { dramaRepository.listEpisodes(dramaId) }
                val drama = dramaDeferred.await()
                val episodes = episodesDeferred.await()
                _state.update {
                    it.copy(
                        drama = drama,
                        episodes = episodes.episodes.sortedBy { ep -> ep.ep },
                        loading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Could not load drama",
                    )
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
            } catch (e: Exception) {
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
            dramaRepository: DramaRepository = DramaRepository.getInstance(
                DramaApi.getInstance(),
            ),
            userRepository: UserRepository = UserRepository.getInstance(
                DramaApi.getInstance(),
                application,
            ),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(application, dramaId, dramaRepository, userRepository) as T
                }
            }
    }
}

