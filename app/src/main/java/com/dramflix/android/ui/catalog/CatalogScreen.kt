package com.dramflix.android.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.GenreChipsRow
import com.dramflix.android.ui.components.HeroFeaturedBanner
import com.dramflix.android.ui.components.ModernDramaGridItem
import com.dramflix.android.ui.components.OfflineBanner
import com.dramflix.android.ui.components.QuickCheckInBanner
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.CoinGold
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.DarkSurfaceVariant
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onOpenDrama: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp),
            )
        },
        containerColor = DarkBackground,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Offline banner
                OfflineBanner(isOnline = isOnline)

                // Refreshing indicator
                if (state.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = BrandViolet,
                        trackColor = DarkSurfaceCard,
                    )
                }

                // Top App Bar: Brand Logo + Server Settings + Coin Balance Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Text(
                            text = "DramaFlix",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoinTopPill(
                            coins = user?.coins ?: 50,
                            streakDay = user?.streakDay ?: 0,
                            canCheckInToday = user?.canCheckInToday ?: true,
                            onClick = viewModel::openRewardsDialog
                        )
                    }
                }

                // Modern Search Box
                OutlinedTextField(
                    value = state.draftQuery,
                    onValueChange = viewModel::onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search title, actor, or genre...", color = TextMutedDark, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BrandVioletLight,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.draftQuery.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceCard,
                        unfocusedContainerColor = DarkSurfaceCard,
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch() }),
                )

                // Error Banner with Direct Reconnect / Server Config Button
                if (state.error != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(14.dp)),
                        color = Color(0xFF450A0A)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ ${state.error.orEmpty()}",
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = viewModel::refresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Retry", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (state.loading && state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BrandViolet)
                    }
                } else if (!state.loading && state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (state.query.isNotEmpty()) "No dramas found matching '${state.query}'" else "Could not load dramas from server.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.query.isNotEmpty()) {
                                    Button(
                                        onClick = viewModel::clearSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                                    ) {
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = viewModel.gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 60.dp, end = 8.dp, start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Hero FeaturedBanner (Only shown when not searching)
                        if (state.query.isEmpty() && state.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HeroFeaturedBanner(
                                    drama = state.items.firstOrNull(),
                                    onClick = { state.items.firstOrNull()?.let { onOpenDrama(it.dramaId) } }
                                )
                            }

                            // Quick 7-Day Check-in Banner
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                QuickCheckInBanner(
                                    user = user,
                                    onOpenRewards = viewModel::openRewardsDialog,
                                    onQuickCheckIn = viewModel::performCheckIn
                                )
                            }

                            // Genre Filter Chips
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                GenreChipsRow(
                                    selectedGenre = state.selectedGenre,
                                    onSelectGenre = viewModel::selectGenre
                                )
                            }

                            // Section Title
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (state.selectedGenre == "All") "Popular Short Dramas" else "${state.selectedGenre} Dramas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "${state.items.size} Available",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedDark
                                    )
                                }
                            }
                        }

                        // Drama Grid Items
                        items(state.items, key = { it.dramaId }) { drama ->
                            ModernDramaGridItem(
                                drama = drama,
                                onClick = { onOpenDrama(drama.dramaId) }
                            )
                        }

                        // Load More Item
                        if (state.nextCursor != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Button(
                                        onClick = viewModel::loadMore,
                                        enabled = !state.loadingMore,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                                        modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        if (state.loadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Load More Dramas", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Rewards Center Dialog Modal
            if (state.showRewardsDialog) {
                RewardsCenterDialog(
                    user = user,
                    isSyncing = isSyncing,
                    onDismiss = viewModel::dismissRewardsDialog,
                    onCheckIn = viewModel::performCheckIn,
                    onWatchAd = {
                        viewModel.dismissRewardsDialog()
                        viewModel.openRewardAdDialog()
                    }
                )
            }

            // Reward Ad Simulation Modal
            if (state.showRewardAdDialog) {
                RewardAdDialog(
                    onDismiss = viewModel::dismissRewardAdDialog,
                    onRewardEarned = viewModel::onAdWatched
                )
            }

        }
    }
}

