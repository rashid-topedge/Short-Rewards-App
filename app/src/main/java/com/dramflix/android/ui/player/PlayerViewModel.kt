package com.dramflix.android.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.api.DramaApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val currentEpisode: Int = 0,
    val playUrl: String? = null,
    val nextEpisode: Int? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val finishedAll: Boolean = false,
    val showCompletionCelebration: Boolean = false,
    val lastRewardEarned: Int = 0,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class PlayerViewModel(
    application: Application,
    private val dramaId: Int,
    initialEpisode: Int,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(
        DramaApi.getInstance(),
    ),
    private val userRepository: UserRepository = UserRepository.getInstance(
        DramaApi.getInstance(),
        application,
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PlayerUiState(currentEpisode = initialEpisode))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile

    private var episodes: List<EpisodeItem> = emptyList()
    private var title: String = ""
    private val rewardedEpisodes = mutableSetOf<Int>()

    init {
        loadCatalog(initialEpisode)
    }

    private fun loadCatalog(startEpisode: Int) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, finishedAll = false) }
            try {
                val drama = dramaRepository.getDrama(dramaId)
                title = drama.title.orEmpty()
                episodes = dramaRepository.listEpisodes(dramaId).episodes.sortedBy { it.ep }
                playEpisode(startEpisode)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Could not load episode",
                    )
                }
            }
        }
    }

    fun playEpisode(episode: Int) {
        val item = episodes.firstOrNull { it.ep == episode }
        val url = item?.playUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            val next = nextPlayableAfter(episode)
            if (next != null) {
                playEpisode(next)
                return
            }
            _state.update {
                it.copy(
                    title = title,
                    currentEpisode = episode,
                    playUrl = null,
                    nextEpisode = null,
                    loading = false,
                    error = "This episode has no media URL.",
                    finishedAll = true,
                    showCompletionCelebration = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                title = title,
                currentEpisode = episode,
                playUrl = url,
                nextEpisode = nextPlayableAfter(episode),
                loading = false,
                error = null,
                finishedAll = false,
                showCompletionCelebration = false,
            )
        }
    }

    fun onEpisodeFinished() {
        val currentEp = _state.value.currentEpisode
        if (currentEp !in rewardedEpisodes) {
            rewardedEpisodes.add(currentEp)
            viewModelScope.launch {
                try {
                    val res = userRepository.rewardEpisode(dramaId, currentEp)
                    _state.update {
                        it.copy(
                            showCompletionCelebration = true,
                            lastRewardEarned = res.coinsEarned,
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            showCompletionCelebration = true,
                            lastRewardEarned = 15,
                        )
                    }
                }
            }
        } else {
            _state.update { it.copy(showCompletionCelebration = true, lastRewardEarned = 0) }
        }
    }

    fun playNextEpisode() {
        _state.update { it.copy(showCompletionCelebration = false) }
        val next = _state.value.nextEpisode
        if (next != null) {
            playEpisode(next)
        }
    }

    fun dismissCelebration() {
        _state.update { it.copy(showCompletionCelebration = false) }
    }

    fun openRewardAd() {
        _state.update { it.copy(showRewardAdDialog = true) }
    }

    fun dismissRewardAd() {
        _state.update { it.copy(showRewardAdDialog = false) }
    }

    fun onAdRewardCompleted() {
        viewModelScope.launch {
            try {
                val res = userRepository.rewardAd("PLAYER_COMPLETION_AD")
                _state.update { it.copy(snackbarMessage = res.message) }
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Claimed ad reward") }
            }
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    private fun nextPlayableAfter(episode: Int): Int? {
        return episodes
            .asSequence()
            .filter { it.ep > episode && !it.playUrl.isNullOrBlank() }
            .map { it.ep }
            .firstOrNull()
    }

    companion object {
        fun factory(
            application: Application,
            dramaId: Int,
            episode: Int,
            repository: DramaRepository = DramaRepository.getInstance(
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
                    return PlayerViewModel(application, dramaId, episode, repository, userRepository) as T
                }
            }
    }
}

