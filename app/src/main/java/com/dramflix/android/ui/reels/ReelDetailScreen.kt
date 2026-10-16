package com.dramflix.android.ui.reels

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelDetailScreen(
    viewModel: ReelDetailViewModel,
    onBack: () -> Unit,
    onOpenDrama: (Int) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val reel = state.reel
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
                        text = reel?.title ?: "Reel",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
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
        when {
            state.loading && reel == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandVioletLight)
                }
            }
            state.error != null && reel == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = viewModel::load,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandVioletLight),
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            reel != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        if (reel.videoUrl.isNotBlank()) {
                            ReelPlayerView(
                                playUrl = reel.videoUrl,
                                onEnded = viewModel::onReelFinished,
                            )
                        } else {
                            AsyncImage(
                                model = reel.thumbnailUrl,
                                contentDescription = reel.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = reel.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatCount(reel.viewCount)} views · ${formatCount(reel.likeCount)} likes",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (reel.dramaId != null) {
                            Button(
                                onClick = { onOpenDrama(reel.dramaId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandVioletLight),
                            ) {
                                Text("Watch Drama", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Button(
                            onClick = viewModel::toggleLike,
                            enabled = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isLiked) BrandRose else DarkSurfaceCard,
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = if (state.isLiked) Color.White else BrandRose,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = if (state.isLiked) "Liked!" else "+Like",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (state.isLiked) Color.White else BrandRose,
                                )
                            }
                        }
                    }

                    if (!reel.synopsis.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Synopsis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                            )
                            Text(
                                text = reel.synopsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (reel.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            reel.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
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

@Composable
private fun ReelPlayerView(
    playUrl: String,
    onEnded: () -> Unit,
) {
    val context = LocalContext.current
    val latestOnEnded by rememberUpdatedState(onEnded)
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(playUrl) {
        player.setMediaItem(MediaItem.fromUri(playUrl))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    latestOnEnded()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = true
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

