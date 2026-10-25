package com.dramflix.android.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.EpisodeCompletedCelebrationDialog
import com.dramflix.android.ui.components.OfflineBanner
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.CoinGold
import com.dramflix.android.ui.theme.CoinGoldLight
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.GoldGradient
import com.dramflix.android.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val ep = state.currentEpisode
                    Column {
                        Text(
                            text = "EP $ep · ${state.title}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Watching Full Episode (+15 Coins on finish)",
                            color = CoinGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                        coins = user?.coins ?: 0,
                        streakDay = user?.streakDay ?: 0,
                        canCheckInToday = user?.canCheckInToday ?: true,
                        onClick = viewModel::openRewardsDialog,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xCC000000),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading -> CircularProgressIndicator(color = BrandViolet)
                state.error != null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (state.nextEpisode != null) {
                            Button(
                                onClick = viewModel::playNextEpisode,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                            ) {
                                Text("Try Next Episode")
                            }
                        }
                    }
                }
                state.finishedAll -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🎉 You've watched all available episodes!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                        ) {
                            Text("Return to Catalog")
                        }
                    }
                }
                !state.playUrl.isNullOrBlank() -> {
                    ExoPlayerView(
                        playUrl = state.playUrl.orEmpty(),
                        episodeKey = state.currentEpisode,
                        initialPositionMs = viewModel.playbackPositionMs,
                        onPositionChanged = { viewModel.playbackPositionMs = it },
                        onEnded = {
                            viewModel.onEpisodeFinished()
                        },
                    )

                    // Offline banner indicator
                    OfflineBanner(
                        isOnline = isOnline,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    // Floating Bottom Fast Switcher
                    if (state.nextEpisode != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = viewModel::playNextEpisode,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC1E1B4B)),
                                modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Next: Ep ${state.nextEpisode}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Episode Completion Celebration Modal
            if (state.showCompletionCelebration) {
                EpisodeCompletedCelebrationDialog(
                    coinsEarned = if (state.lastRewardEarned > 0) state.lastRewardEarned else 15,
                    nextEpisode = state.nextEpisode,
                    onPlayNext = viewModel::playNextEpisode,
                    onWatchAdForDouble = {
                        viewModel.dismissCelebration()
                        viewModel.openRewardAd()
                    },
                    onDismiss = viewModel::dismissCelebration
                )
            }

            // Rewarded Ad Modal
            if (state.showRewardAdDialog) {
                RewardAdDialog(
                    onDismiss = viewModel::dismissRewardAd,
                    onRewardEarned = viewModel::onAdRewardCompleted
                )
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
                        viewModel.openRewardAd()
                    }
                )
            }
        }
    }
}

@Composable
private fun ExoPlayerView(
    playUrl: String,
    episodeKey: Int,
    initialPositionMs: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    onEnded: () -> Unit,
) {
    val context = LocalContext.current
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnPositionChanged by rememberUpdatedState(onPositionChanged)
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(playUrl, episodeKey) {
        player.setMediaItem(MediaItem.fromUri(playUrl))
        if (initialPositionMs > 0L) {
            player.seekTo(initialPositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val pos = player.currentPosition
                if (pos > 0L) {
                    latestOnPositionChanged(pos)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    latestOnEnded()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            val pos = player.currentPosition
            if (pos > 0L) {
                latestOnPositionChanged(pos)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
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
