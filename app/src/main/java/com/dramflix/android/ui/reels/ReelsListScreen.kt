package com.dramflix.android.ui.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.ui.components.CoinTopPill
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
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsListScreen(
    viewModel: ReelsListViewModel,
    onOpenReel: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()

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
                        text = "Short Reels",
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
                                    brush = if (user?.canCheckInToday == true) BrandGradient else Brush.linearGradient(listOf(DarkBorder, DarkBorder)),
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search reels...", color = TextMutedDark, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Search",
                        tint = BrandVioletLight,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard,
                    focusedBorderColor = BrandViolet,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                ),
            )

            ReelsSortMenu(
                currentSort = state.sort,
                onSortSelected = viewModel::setSort,
                modifier = Modifier.fillMaxWidth(),
            )

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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (state.query.isNotEmpty()) "No reels found matching '${state.query}'" else "Could not load reels from server.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                        )
                        if (state.query.isNotEmpty()) {
                            Button(
                                onClick = viewModel::clearSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            ) {
                                Text("Clear Search")
                            }
                        }
                        Button(
                            onClick = viewModel::openServerConfigDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                            modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        ) {
                            Text("⚙️ Configure Server IP:Port", color = TextPrimaryDark)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.items, key = { _, item -> item.reelId }) { _, reel ->
                        ReelsGridItem(
                            reel = reel,
                            onClick = { onOpenReel(reel.reelId) },
                        )
                    }

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
                                    modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text("Load More Reels", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
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
private fun ReelsSortMenu(
    currentSort: String,
    onSortSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { expanded = true },
        color = DarkSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (currentSort) {
                    "trending" -> "Trending"
                    "random" -> "Random"
                    else -> "Newest"
                },
                color = TextPrimaryDark,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = "Sort ▼",
                color = BrandVioletLight,
                fontSize = 11.sp,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf("newest" to "Newest", "trending" to "Trending", "random" to "Random").forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSortSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ReelsGridItem(
    reel: ReelItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = reel.thumbnailUrl ?: reel.videoUrl,
                contentDescription = reel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0B0E14)),
                            startY = 120f,
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = formatCount(reel.durationSeconds / 60) + "m",
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = reel.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BrandVioletLight,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = formatCount(reel.viewCount) + " views",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark,
                )
                if (reel.likeCount > 0) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = BrandRose,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = formatCount(reel.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                    )
                }
            }
        }
    }
}

