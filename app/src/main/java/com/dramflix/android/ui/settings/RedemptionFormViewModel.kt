package com.dramflix.android.ui.settings

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.model.CreateRedemptionRequest
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import com.dramflix.android.data.api.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RedemptionFormUiState(
    val coinsAmount: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val paymentMethod: String = "Easypaisa",
    val bankName: String = "",
    val coinsError: String? = null,
    val loading: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val showRewardsDialog: Boolean = false,
    val showRewardAdDialog: Boolean = false,
    /** Server requires a Google-linked account to redeem (403 GOOGLE_LINK_REQUIRED). */
    val requiresGoogleLink: Boolean = false,
)

data class RedemptionFormSettings(
    val minRedeemCoins: Int,
    val coinToPkrRate: Double,
    val supportedPaymentMethods: List<String>,
)

class RedemptionFormViewModel(
    application: Application,
    private val userRepository: UserRepository = UserRepository.getInstance(application),
    networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RedemptionFormUiState())
    val state: StateFlow<RedemptionFormUiState> = _state.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
    val isUserSyncing: StateFlow<Boolean> = userRepository.isSyncing

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val scrollState = ScrollState(initial = 0)

    private val _settings = MutableStateFlow<RedemptionFormSettings?>(null)
    val settings: StateFlow<RedemptionFormSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                userRepository.sync()
            } catch (_: Exception) {
            }
            refreshSettings()
        }
    }

    fun setCoinsAmount(value: String) {
        val amount = value.filter { it.isDigit() }
        _state.update { it.copy(coinsAmount = amount, coinsError = null) }
        validateCoins(amount)
    }

    fun setAccountTitle(value: String) {
        _state.update { it.copy(accountTitle = value, error = null) }
    }

    fun setAccountNumber(value: String) {
        _state.update { it.copy(accountNumber = value, error = null) }
    }

    fun setPaymentMethod(value: String) {
        _state.update { it.copy(paymentMethod = value) }
    }

    fun setBankName(value: String) {
        _state.update { it.copy(bankName = value, error = null) }
    }

    fun openRewardsDialog() {
        _state.update { it.copy(showRewardsDialog = true) }
        viewModelScope.launch {
            runCatching { userRepository.sync() }
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

    fun showMessage(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    /** Linked: hide the prompt and refresh the profile; the user can submit again. */
    fun onGoogleLinked() {
        _state.update { it.copy(requiresGoogleLink = false) }
        viewModelScope.launch { runCatching { userRepository.sync(force = true) } }
    }

    fun submitRedemption() {
        // Guard against double taps sending two redemption requests.
        if (_state.value.loading || _state.value.submitted) return
        val amount = _state.value.coinsAmount.toIntOrNull() ?: 0
        val minVal = _settings.value?.minRedeemCoins ?: 500
        val coins = userProfile.value?.coins ?: 0
        if (amount < minVal) {
            _state.update { it.copy(coinsError = "Minimum $minVal coins required") }
            return
        }
        if (amount > coins) {
            _state.update { it.copy(coinsError = "Not enough coins") }
            return
        }
        val form = _state.value
        val missing = when {
            form.accountTitle.isBlank() -> "Enter the account title"
            form.accountNumber.isBlank() -> "Enter the account number"
            form.paymentMethod.equals("Bank Transfer", ignoreCase = true) && form.bankName.isBlank() ->
                "Enter the bank name"
            else -> null
        }
        if (missing != null) {
            _state.update { it.copy(error = missing) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val payment = _state.value.paymentMethod.ifBlank { "Easypaisa" }
                val response = userRepository.createRedemption(
                    CreateRedemptionRequest(
                        userId = userRepository.getUserId(),
                        coinsAmount = amount,
                        paymentMethod = payment,
                        accountTitle = _state.value.accountTitle.trim(),
                        accountNumber = _state.value.accountNumber.trim(),
                        bankName = _state.value.bankName.trim().ifBlank { null },
                    ),
                )
                if (response.success) {
                    _state.update {
                        it.copy(
                            loading = false,
                            submitted = true,
                            snackbarMessage = response.message ?: "Redemption request submitted",
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = response.message ?: response.error ?: "Redemption request failed",
                        )
                    }
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(loading = false) }
                throw e
            } catch (e: ApiException) {
                if (e.code == ApiException.GOOGLE_LINK_REQUIRED) {
                    _state.update {
                        it.copy(
                            loading = false,
                            requiresGoogleLink = true,
                            error = null,
                        )
                    }
                } else {
                    // ApiException carries the server's own reason (e.g. "Insufficient coins").
                    _state.update { it.copy(loading = false, error = e.message ?: "Server error") }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Server error")
                }
            }
        }
    }

    private fun validateCoins(amount: String) {
        val intVal = amount.toIntOrNull()
        val minVal = _settings.value?.minRedeemCoins ?: 500
        when {
            amount.isBlank() -> _state.update { it.copy(coinsError = "Enter coin amount to continue") }
            intVal != null && intVal <= 0 -> _state.update { it.copy(coinsError = "Must be a positive number") }
            intVal != null && intVal < minVal -> _state.update { it.copy(coinsError = "Minimum $minVal coins required") }
            else -> _state.update { it.copy(coinsError = null) }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch {
            try {
                val res = userRepository.getSettings()
                _settings.value = RedemptionFormSettings(
                    minRedeemCoins = res.minRedeemCoins,
                    coinToPkrRate = res.coinToPkrRate,
                    supportedPaymentMethods = res.supportedPaymentMethods,
                )
                if (_state.value.paymentMethod.isBlank()) {
                    _state.update {
                        it.copy(paymentMethod = res.supportedPaymentMethods.firstOrNull() ?: "Easypaisa")
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RedemptionFormViewModel(application) as T
                }
            }
    }
}
