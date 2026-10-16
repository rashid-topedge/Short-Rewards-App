package com.dramflix.android.ui.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.DramaCard
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val draftQuery: String = "",
    val selectedGenre: String = "All",
    val items: List<DramaCard> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,

    val snackbarMessage: String? = null,
)

class CatalogViewModel(
    application: Application,
    private val dramaRepository: DramaRepository = DramaRepository.getInstance(
        com.dramflix.android.data.api.DramaApi.getInstance(),
    ),
    private val userRepository: UserRepository = UserRepository.getInstance(
        com.dramflix.android.data.api.DramaApi.getInstance(),
        application,
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CatalogUiState(loading = true))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    init {
        refresh()
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (e: Exception) {
                // keep existing behavior silent on first failure
            }
        }
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draftQuery = value) }
    }

    fun submitSearch() {
        val q = _state.value.draftQuery.trim()
        _state.update { it.copy(query = q, draftQuery = q) }
        refresh()
    }

    fun clearSearch() {
        _state.update { it.copy(query = "", draftQuery = "") }
        refresh()
    }

    fun selectGenre(genre: String) {
        val cleanGenre = genre.split(" ").firstOrNull() ?: genre
        _state.update {
            it.copy(
                selectedGenre = genre,
                query = if (genre == "All") "" else cleanGenre,
                draftQuery = if (genre == "All") "" else cleanGenre,
            )
        }
        refresh()
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

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, items = emptyList(), nextCursor = null)
            }
            try {
                val response = dramaRepository.listDramas(query = _state.value.query).first()
                _state.update {
                    it.copy(
                        items = response.items,
                        nextCursor = response.nextCursor,
                        loading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Could not load dramas",
                    )
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loadingMore || _state.value.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            try {
                val response = dramaRepository.listDramas(
                    cursor = cursor,
                    query = _state.value.query,
                ).first()
                _state.update { current ->
                    if (current.nextCursor != cursor) return@update current
                    val seen = current.items.map { it.dramaId }.toHashSet()
                    val appended = response.items.filter { it.dramaId !in seen }
                    current.copy(
                        items = current.items + appended,
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

