package com.dramflix.android.ui.settings

import android.content.res.Configuration
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dramflix.android.data.model.RedemptionRequest
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.components.ServerConfigDialog
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
import com.dramflix.android.ui.theme.SuccessGreen
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenRedemptionForm: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = viewModel::openServerConfigDialog,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(50)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Server URL & Port Settings",
                                tint = BrandVioletLight,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        CoinTopPill(
                            coins = user?.coins ?: 50,
                            streakDay = user?.streakDay ?: 0,
                            canCheckInToday = user?.canCheckInToday ?: true,
                            onClick = viewModel::openRewardsDialog,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF261D08),
                                            Color(0xFF3B2807),
                                        ),
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (user?.canCheckInToday == true) GoldGradient else Brush.linearGradient(listOf(DarkBorder, DarkBorder)),
                                    shape = RoundedCornerShape(20),
                                ),
                        )
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // User profile card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                            )
                            Text(
                                text = user?.userId ?: "Unknown",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                            )
                        }
                        IconButton(onClick = viewModel::syncUser) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BrandVioletLight,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync user",
                                    tint = BrandVioletLight,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Coins",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = formatCount(user?.coins ?: 0) + " 🪙",
                                fontWeight = FontWeight.Bold,
                                color = CoinGoldLight,
                                fontSize = 18.sp,
                            )
                        }
                        val currentUser = user
                        if (currentUser?.streakDay != null && currentUser.streakDay > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF4C1D95).copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = "Streak",
                                        tint = BrandRose,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Day ${currentUser.streakDay}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Last Check-in",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = user?.lastCheckInDate ?: "Never",
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                            )
                        }
                        if (user?.canCheckInToday == true) {
                            Button(
                                onClick = viewModel::performCheckIn,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("Check In", fontSize = 12.sp, color = Color.White)
                            }
                        } else {
                            Text(
                                text = "Done ✓",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            // Rewards quick actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Earn Coins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                viewModel.dismissRewardsDialog()
                                viewModel.openRewardAdDialog()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Watch Ad +25",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = viewModel::openRewardsDialog,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = BrandVioletLight,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Rewards Hub",
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = viewModel::performCheckIn,
                            enabled = user?.canCheckInToday == true && !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (user?.canCheckInToday == true) BrandViolet else TextSecondaryDark,
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = if (user?.canCheckInToday == true) BrandViolet else TextSecondaryDark,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Check In Now",
                                    fontSize = 12.sp,
                                    color = if (user?.canCheckInToday == true) BrandViolet else TextSecondaryDark,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenRedemptionForm,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = CoinGold,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Redeem Coins",
                                    fontSize = 12.sp,
                                    color = CoinGoldLight,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // Redemption settings
            val supportedMethods = settings?.supportedPaymentMethods ?: listOf("Easypaisa", "JazzCash", "Bank Transfer", "Raast")
            RedemptionSettingsSection(
                settings = settings,
                paymentMethods = supportedMethods,
                onPaymentMethodSelected = viewModel::setPaymentMethod,
            )

            RedemptionHistorySection(
                loading = state.loadingRedemptions,
                error = state.errorRedemptions,
                items = state.redemptions,
                onRetry = viewModel::loadRedemptions,
            )

            // App preferences
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Animations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                            )
                            Text(
                                text = "Enable UI animations and transitions",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = state.animationsEnabled,
                            onCheckedChange = viewModel::setAnimationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrandVioletLight,
                                checkedTrackColor = BrandViolet.copy(alpha = 0.5f),
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = DarkSurfaceVariant,
                            ),
                        )
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Server URL",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                            )
                            Text(
                                text = state.serverUrl ?: "Not configured",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = viewModel::openServerConfigDialog) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure server",
                                tint = BrandVioletLight,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        if (state.showRewardsDialog) {
            RewardsCenterDialog(
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
            RewardAdDialog(
                onDismiss = viewModel::dismissRewardAdDialog,
                onRewardEarned = viewModel::onAdWatched,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedemptionSettingsSection(
    settings: SystemSettingsDto?,
    paymentMethods: List<String>,
    onPaymentMethodSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(paymentMethods.firstOrNull() ?: "Easypaisa") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Redemption Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
            )

            Text(
                text = "Minimum " + (settings?.minRedeemCoins ?: 500) + " coins → " + (settings?.minRedeemPkr?.let { "Rs. ${String.format(Locale.US, "%.2f", it)}" } ?: "Rs. 0.00"),
                color = CoinGoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = "Rate: 1 coin = " + (settings?.coinToPkrRate?.let { "Rs. ${String.format(Locale.US, "%.2f", it)}" } ?: "Rs. 0.00"),
                color = TextSecondaryDark,
                fontSize = 12.sp,
            )

            HorizontalDivider(color = DarkBorder)

            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedMethod,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    paymentMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method) },
                            onClick = {
                                selectedMethod = method
                                onPaymentMethodSelected(method)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionHistorySection(
    loading: Boolean,
    error: String?,
    items: List<RedemptionRequest>,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Redemption History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                )
                IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh redemptions",
                        tint = BrandVioletLight,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            when {
                loading && items.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = BrandVioletLight,
                        strokeWidth = 2.dp,
                    )
                }
                error != null && items.isEmpty() -> {
                    Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                }
                items.isEmpty() -> {
                    Text("No redemption requests yet.", color = TextSecondaryDark, fontSize = 13.sp)
                }
                else -> {
                    items.take(8).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.coinsAmount} coins · Rs. ${item.pkrAmount}",
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${item.paymentMethod} · ${item.accountNumber}",
                                    color = TextMutedDark,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = item.status,
                                color = when (item.status) {
                                    "PAID" -> SuccessGreen
                                    "REJECTED" -> BrandRose
                                    else -> CoinGoldLight
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

