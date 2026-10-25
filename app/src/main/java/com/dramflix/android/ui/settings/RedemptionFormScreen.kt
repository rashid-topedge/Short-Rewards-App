package com.dramflix.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.OfflineBanner
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.CoinGold
import com.dramflix.android.ui.theme.CoinGoldLight
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.DarkSurfaceVariant
import com.dramflix.android.ui.theme.GoldGradient
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionFormScreen(
    viewModel: RedemptionFormViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(state.submitted) {
        if (state.submitted) {
            onSubmitted()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Redeem Coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    CoinTopPill(
                        coins = user?.coins ?: 0,
                        streakDay = user?.streakDay ?: 0,
                        canCheckInToday = user?.canCheckInToday ?: true,
                        onClick = viewModel::openRewardsDialog,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground.copy(alpha = 0.95f),
                ),
            )
        },
        containerColor = DarkBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OfflineBanner(isOnline = isOnline)

            // Balance display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Your Balance",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "${user?.coins ?: 0} 🪙",
                            fontWeight = FontWeight.Bold,
                            color = CoinGoldLight,
                            fontSize = 22.sp,
                        )
                    }
                    val currentSettings = settings
                    if (currentSettings != null) {
                        Text(
                            text = "Min " + currentSettings.minRedeemCoins + " coins",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(viewModel.scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RedemptionTextField(
                    label = "Coins to Redeem",
                    value = state.coinsAmount,
                    onValueChange = viewModel::setCoinsAmount,
                    placeholder = "Enter coin amount",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = "🪙",
                    suffixColor = CoinGold,
                    isError = state.coinsError != null,
                    errorMessage = state.coinsError,
                    modifier = Modifier.fillMaxWidth(),
                )

                RedemptionTextField(
                    label = "Account Title",
                    value = state.accountTitle,
                    onValueChange = viewModel::setAccountTitle,
                    placeholder = "e.g., John Doe",
                    modifier = Modifier.fillMaxWidth(),
                )

                RedemptionTextField(
                    label = "Account Number",
                    value = state.accountNumber,
                    onValueChange = viewModel::setAccountNumber,
                    placeholder = "Phone or account number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                RedemptionTextField(
                    label = "Payment Method",
                    value = state.paymentMethod,
                    onValueChange = viewModel::setPaymentMethod,
                    placeholder = settings?.supportedPaymentMethods?.joinToString(", ") ?: "Easypaisa, JazzCash, Bank Transfer",
                    modifier = Modifier.fillMaxWidth(),
                )

                RedemptionTextField(
                    label = "Bank Name (optional)",
                    value = state.bankName,
                    onValueChange = viewModel::setBankName,
                    placeholder = "Required for Bank Transfer",
                    modifier = Modifier.fillMaxWidth(),
                )

                val settingsSnapshot = settings
                if (settingsSnapshot != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        color = DarkSurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Estimated PKR",
                                    color = TextSecondaryDark,
                                    fontSize = 13.sp,
                                )
                                val estimate = (state.coinsAmount.toDoubleOrNull() ?: 0.0) * settingsSnapshot.coinToPkrRate
                                Text(
                                    text = if (state.coinsAmount.isNotBlank()) "Rs. ${String.format(Locale.US, "%.2f", estimate)}" else "--",
                                    fontWeight = FontWeight.Bold,
                                    color = CoinGoldLight,
                                    fontSize = 16.sp,
                                )
                            }
                            Text(
                                text = "Rate: 1 coin = " + String.format(Locale.US, "%.2f", settingsSnapshot.coinToPkrRate),
                                color = TextMutedDark,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF450A0A))
                            .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(12.dp)),
                        color = Color(0xFF450A0A),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = state.error.orEmpty(),
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Button(
                    onClick = viewModel::submitRedemption,
                    enabled = !state.loading && state.coinsAmount.isNotBlank() && state.coinsAmount.toIntOrNull()?.let { it > 0 } == true && state.accountTitle.isNotBlank() && state.accountNumber.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Submit Redemption Request",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                ) {
                    Text("Cancel", fontSize = 13.sp, color = TextSecondaryDark)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (state.showRewardsDialog) {
            com.dramflix.android.ui.components.RewardsCenterDialog(
                user = user,
                isSyncing = isSyncing,
                onDismiss = viewModel::dismissRewardsDialog,
                onCheckIn = viewModel::performCheckIn,
                onWatchAd = {
                    viewModel.dismissRewardsDialog()
                    viewModel.openRewardAdDialog()
                },
            )
        }

        if (state.showRewardAdDialog) {
            com.dramflix.android.ui.components.RewardAdDialog(
                onDismiss = viewModel::dismissRewardAdDialog,
                onRewardEarned = viewModel::onAdWatched,
            )
        }
    }
}

@Composable
private fun RedemptionTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    suffix: String? = null,
    suffixColor: Color = TextSecondaryDark,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, color = TextMutedDark, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard,
                    focusedBorderColor = if (isError) BrandRose else BrandViolet,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                ),
                keyboardOptions = keyboardOptions,
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    color = suffixColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = BrandRose,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

