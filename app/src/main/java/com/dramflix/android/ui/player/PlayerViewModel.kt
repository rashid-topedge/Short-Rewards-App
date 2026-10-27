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
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    val snackbarMessage: String? = null,
)

class PlayerViewModel(
    application: Application,
    private val dramaId: Int,
    initialEpisode: Int,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PlayerUiState(currentEpisode = initialEpisode))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    var playbackPositionMs: Long = 0L

    private var episodes: List<EpisodeItem> = emptyList()
    private var title: String = ""
    private val rewardedEpisodes = mutableSetOf<Int>()

    init {
        loadCatalog(initialEpisode)
    }

    /**
     * Tries network first via backward-compatible methods that
     * internally fall back to Room cache when offline.
     */
    private var loadJob: Job? = null

    private fun loadCatalog(startEpisode: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, finishedAll = false) }
            try {
                val drama = dramaRepository.getDrama(dramaId)
                title = drama.title.orEmpty()
                episodes = dramaRepository.listEpisodes(dramaId).episodes.sortedBy { it.ep }
                playEpisode(startEpisode)
            } catch (e: CancellationException) {
                throw e
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
        if (_state.value.currentEpisode != episode) {
            playbackPositionMs = 0L
        }
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
                    // The reward response already carries the updated balance; no extra sync.
                    _state.update {
                        it.copy(
                            showCompletionCelebration = true,
                            lastRewardEarned = res.coinsEarned,
                        )
                    }
                } catch (e: CancellationException) {
                    rewardedEpisodes.remove(currentEp)
                    throw e
                } catch (e: Exception) {
                    // Previously this showed a fake "+15 coins" even though nothing was
                    // credited. Show the real reason and allow the claim to be retried.
                    rewardedEpisodes.remove(currentEp)
                    _state.update {
                        it.copy(
                            showCompletionCelebration = true,
                            lastRewardEarned = 0,
                            snackbarMessage = e.message ?: "Couldn't claim the episode reward",
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

    fun openRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = true) }
        viewModelScope.launch {
            try { userRepository.sync() } catch (_: Exception) {}
        }
    }

    fun dismissRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = false) }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Failed to claim ad reward") }
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
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(application, dramaId, episode) as T
                }
            }
    }
}
