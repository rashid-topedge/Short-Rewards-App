package com.dramflix.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.RedemptionRequest
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String? = null,
    val paymentMethod: String = "Easypaisa",
    val animationsEnabled: Boolean = true,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,

    val snackbarMessage: String? = null,
    val redemptions: List<RedemptionRequest> = emptyList(),
    val loadingRedemptions: Boolean = false,
    val errorRedemptions: String? = null,
)

class SettingsViewModel(
    application: Application,
    private val userRepository: UserRepository = UserRepository.getInstance(
        com.dramflix.android.data.api.DramaApi.getInstance(),
        application,
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing
    val settings: StateFlow<SystemSettingsDto?> = userRepository.settings

    init {
        loadServerUrl()
        loadRedemptions()
        viewModelScope.launch {
            runCatching { userRepository.sync() }
            runCatching { userRepository.refreshSettings() }
        }
    }

    private fun loadServerUrl() {
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = com.dramflix.android.BuildConfig.API_BASE_URL) }
        }
    }

    fun syncUser() {
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = e.message ?: "Sync failed") }
            }
        }
    }

    fun setPaymentMethod(method: String) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        _state.update { it.copy(animationsEnabled = enabled) }
    }

    fun openRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = true) }
        syncUser()
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

    fun loadRedemptions() {
        viewModelScope.launch {
            _state.update { it.copy(loadingRedemptions = true, errorRedemptions = null) }
            try {
                val list = userRepository.listRedemptions()
                _state.update { it.copy(redemptions = list, loadingRedemptions = false, errorRedemptions = null) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingRedemptions = false, errorRedemptions = e.message ?: "Failed to load redemptions") }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(application) as T
                }
            }
    }
}

