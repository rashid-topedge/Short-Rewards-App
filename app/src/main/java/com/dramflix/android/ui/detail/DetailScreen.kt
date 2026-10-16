package com.dramflix.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.ui.components.CoinTopPill
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
import com.dramflix.android.ui.util.formatCount

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (ep: Int, playUrl: String, title: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val drama = state.drama

    val snackbarHostState = remember { SnackbarHostState() }
    var synopsisExpanded by remember { mutableStateOf(false) }

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
                        text = drama?.title ?: "Drama Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    CoinTopPill(
                        coins = user?.coins ?: 50,
                        streakDay = user?.streakDay ?: 0,
                        canCheckInToday = user?.canCheckInToday ?: true,
                        onClick = viewModel::openRewardsDialog,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground.copy(alpha = 0.95f),
                ),
            )
        },
        containerColor = DarkBackground,
    ) { padding ->
        when {
            state.loading && drama == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandViolet)
                }
            }

            state.error != null && drama == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = viewModel::load,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            drama != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header Backdrop Banner with Blur Effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        AsyncImage(
                            model = drama.cover,
                            contentDescription = drama.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x800B0E14),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        // Poster Card Floating on Left
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AsyncImage(
                                model = drama.cover,
                                contentDescription = drama.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = drama.title ?: "Untitled",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = formatCount(drama.playCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark,
                                        )
                                    }

                                    if (drama.likeCount > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = BrandRose,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = formatCount(drama.likeCount),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondaryDark,
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${drama.episodeCount} eps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark,
                                    )
                                }

                                if (drama.latestEpisodeLabel != null) {
                                    Text(
                                        text = drama.latestEpisodeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandVioletLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val firstEp = state.episodes.firstOrNull { !it.playUrl.isNullOrBlank() }
                        Button(
                            onClick = {
                                if (firstEp != null && !firstEp.playUrl.isNullOrBlank()) {
                                    onPlayEpisode(
                                        firstEp.ep,
                                        firstEp.playUrl,
                                        drama.title ?: "Episode ${firstEp.ep}"
                                    )
                                }
                            },
                            enabled = firstEp != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (firstEp != null) "Watch Ep ${firstEp.ep}" else "Unavailable",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Watchlist Button
                        IconButton(
                            onClick = viewModel::toggleFavorite,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (state.isFavorite) CoinGold else Color.White
                            )
                        }
                    }

                    // Tags FlowRow
                    if (drama.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            drama.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Synopsis
                    if (!drama.synopsis.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Synopsis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = drama.synopsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                            )
                            if (drama.synopsis.length > 120) {
                                Text(
                                    text = if (synopsisExpanded) "Show Less ▲" else "Read More ▼",
                                    fontSize = 11.sp,
                                    color = BrandVioletLight,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                )
                            }
                        }
                    }

                    // Episodes Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes (${state.episodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = CoinGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+15 Coins / Ep Watched",
                                fontSize = 11.sp,
                                color = CoinGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Episodes Grid
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.episodes.forEach { episode ->
                            val playable = !episode.playUrl.isNullOrBlank()
                            EpisodeChipItem(
                                episode = episode,
                                playable = playable,
                                onClick = {
                                    val url = episode.playUrl ?: return@EpisodeChipItem
                                    onPlayEpisode(
                                        episode.ep,
                                        url,
                                        drama.title ?: "Episode ${episode.ep}",
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun EpisodeChipItem(
    episode: EpisodeItem,
    playable: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (playable) DarkSurfaceCard else DarkBackground)
            .border(
                width = 1.dp,
                color = if (playable) DarkBorder else Color(0x15FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = playable, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (playable) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BrandVioletLight,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "EP ${episode.ep}",
                fontSize = 13.sp,
                fontWeight = if (playable) FontWeight.SemiBold else FontWeight.Normal,
                color = if (playable) TextPrimaryDark else TextMutedDark
            )
        }
    }
}
