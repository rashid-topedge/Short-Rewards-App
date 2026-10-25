package com.dramflix.android.ui.reels

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.ui.components.CoinTopPill
import com.dramflix.android.ui.components.OfflineBanner
import com.dramflix.android.ui.components.RewardAdDialog
import com.dramflix.android.ui.components.RewardsCenterDialog
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsListScreen(
    viewModel: ReelsListViewModel,
    onOpenReel: (Int) -> Unit = {},
    onOpenDrama: (Int) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isUserSyncing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading && state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(color = BrandVioletLight, strokeWidth = 3.dp)
                            Text(
                                text = "Loading Reels...",
                                color = TextSecondaryDark,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
                !state.loading && state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = BrandVioletLight,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(
                                text = state.error ?: "No reels available right now.",
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = viewModel::refresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                else -> {
                    val initialPage = viewModel.currentPage.coerceIn(0, (state.items.size - 1).coerceAtLeast(0))
                    val pagerState = rememberPagerState(
                        initialPage = initialPage,
                        pageCount = { state.items.size },
                    )

                    // Infinite pagination & automatic view tracking on page transition
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            viewModel.currentPage = page
                            state.items.getOrNull(page)?.let { currentReel ->
                                viewModel.recordView(currentReel.reelId)
                            }
                            if (page >= state.items.size - 2 && state.nextCursor != null && !state.loadingMore) {
                                viewModel.loadMore()
                            }
                        }
                    }

                    LaunchedEffect(state.items) {
                        state.items.getOrNull(pagerState.currentPage)?.let { currentReel ->
                            viewModel.recordView(currentReel.reelId)
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        beyondViewportPageCount = 0,
                        modifier = Modifier.fillMaxSize(),
                        key = { page -> state.items.getOrNull(page)?.reelId ?: page },
                    ) { page ->
                        val reel = state.items.getOrNull(page)
                        if (reel != null) {
                            val isCurrentPage = pagerState.currentPage == page
                            val isLiked = state.likedReelIds.contains(reel.reelId)
                            val displayLikeCount = state.likeCounts[reel.reelId] ?: reel.likeCount
                            val displayViewCount = state.viewCounts[reel.reelId] ?: reel.viewCount

                            ReelPageItem(
                                reel = reel,
                                isCurrentPage = isCurrentPage,
                                isMuted = state.isMuted,
                                isLiked = isLiked,
                                likeCount = displayLikeCount,
                                viewCount = displayViewCount,
                                onToggleLike = { viewModel.toggleLike(reel.reelId) },
                                onToggleMute = viewModel::toggleMute,
                                onOpenDrama = { dramaId -> onOpenDrama(dramaId) },
                                onShare = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TITLE, reel.title)
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "🔥 Check out '${reel.title}' on Dramflix!\n${reel.videoUrl}",
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                                },
                                onFinished = { viewModel.onReelFinished(reel.reelId) },
                            )
                        }
                    }

                    // Instagram-style Transparent Gradient Top Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Reels",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 0.5.sp,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CoinTopPill(
                                coins = user?.coins ?: 0,
                                streakDay = user?.streakDay ?: 0,
                                canCheckInToday = user?.canCheckInToday ?: true,
                                onClick = viewModel::openRewardsDialog,
                            )
                        }
                    }

                    // Top Offline Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                    ) {
                        OfflineBanner(isOnline = isOnline)
                    }
                }
            }
        }

        // Dialogs
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
private fun ReelPageItem(
    reel: ReelItem,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    isLiked: Boolean,
    likeCount: Int,
    viewCount: Int,
    onToggleLike: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenDrama: (Int) -> Unit,
    onShare: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    // Heart pop animation
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    // Play/Pause indicator animation
    val playPauseAlpha = remember { Animatable(0f) }

    val latestOnFinished by rememberUpdatedState(onFinished)

    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Strictly ensure only the current page plays audio/video; inactive page is immediately paused & silenced
    LaunchedEffect(isCurrentPage, isPlaying, reel.videoUrl) {
        if (isCurrentPage) {
            if (player.mediaItemCount == 0) {
                player.setMediaItem(MediaItem.fromUri(reel.videoUrl))
                player.prepare()
            }
            player.playWhenReady = isPlaying
            if (isPlaying) {
                player.play()
            } else {
                player.pause()
            }
        } else {
            player.playWhenReady = false
            player.pause()
            player.seekTo(0)
        }
    }

    LaunchedEffect(isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(lifecycleOwner, player, isCurrentPage) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    player.playWhenReady = false
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isCurrentPage && isPlaying) {
                        player.playWhenReady = true
                        player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Monitor playback progress & trigger coin reward on 90% completion
    var rewardTriggeredForCurrentReel by remember(reel.reelId) { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage, isPlaying) {
        if (isCurrentPage) {
            while (isActive) {
                val dur = player.duration
                val cur = player.currentPosition
                if (dur > 0) {
                    playbackProgress = (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    if (cur >= dur * 0.9f && !rewardTriggeredForCurrentReel) {
                        rewardTriggeredForCurrentReel = true
                        latestOnFinished()
                    }
                }
                delay(120)
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING || state == Player.STATE_IDLE)
            }

            override fun onRenderedFirstFrame() {
                isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.reelId) {
                detectTapGestures(
                    onTap = {
                        isPlaying = !isPlaying
                        coroutineScope.launch {
                            playPauseAlpha.snapTo(0.9f)
                            playPauseAlpha.animateTo(0f, tween(500))
                        }
                    },
                    onDoubleTap = {
                        if (!isLiked) {
                            onToggleLike()
                        }
                        coroutineScope.launch {
                            heartScale.snapTo(0.2f)
                            heartAlpha.snapTo(1f)
                            launch {
                                heartScale.animateTo(1.3f, tween(180, easing = FastOutSlowInEasing))
                                heartScale.animateTo(1.0f, tween(100))
                            }
                            delay(500)
                            heartAlpha.animateTo(0f, tween(250))
                        }
                    },
                )
            },
    ) {
        // Fullscreen ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        // Thumbnail preview during buffering/loading
        if (isBuffering && !reel.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = reel.thumbnailUrl,
                contentDescription = reel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = BrandVioletLight,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // Bottom gradient scrim for high readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.9f),
                        ),
                    ),
                ),
        )

        // Center Animated Heart on double-tap
        if (heartAlpha.value > 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = BrandRose,
                    modifier = Modifier
                        .size(100.dp)
                        .scale(heartScale.value)
                        .alpha(heartAlpha.value),
                )
            }
        }

        // Center Play/Pause brief indicator
        if (playPauseAlpha.value > 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .alpha(playPauseAlpha.value),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        // Right-Side Action Column (elevated above bottom nav)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 95.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Like Button
            ReelActionButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                tint = if (isLiked) BrandRose else Color.White,
                label = formatCount(likeCount),
                onClick = onToggleLike,
            )

            // Watch Drama Button (if attached)
            if (reel.dramaId != null) {
                ReelActionButton(
                    icon = Icons.Default.Movie,
                    tint = BrandVioletLight,
                    label = "Drama",
                    onClick = { onOpenDrama(reel.dramaId) },
                )
            }

            // Share Button
            ReelActionButton(
                icon = Icons.Default.Share,
                tint = Color.White,
                label = "Share",
                onClick = onShare,
            )

            // Mute / Unmute Toggle Button
            ReelActionButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                tint = if (isMuted) Color(0xFFFF6B6B) else Color.White,
                label = if (isMuted) "Muted" else "Sound",
                onClick = onToggleMute,
            )
        }

        // Bottom-Left Info Overlay (elevated above bottom nav)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 95.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Drama Badge Tag (clickable)
            if (reel.dramaId != null && !reel.dramaTitle.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, BrandVioletLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onOpenDrama(reel.dramaId) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = BrandVioletLight,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = buildString {
                            append(reel.dramaTitle)
                            reel.episodeNumber?.let { append(" · Ep $it") }
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Reel Title
            Text(
                text = reel.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = if (isSynopsisExpanded) 6 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Synopsis (with expand toggle)
            if (!reel.synopsis.isNullOrBlank()) {
                Column {
                    Text(
                        text = reel.synopsis,
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = if (isSynopsisExpanded) 8 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isSynopsisExpanded) "Show less" else "more",
                        color = BrandVioletLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            .padding(top = 2.dp),
                    )
                }
            }

            // Views & Tags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${formatCount(viewCount)} views",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
                if (reel.tags.isNotEmpty()) {
                    Text(
                        text = reel.tags.take(2).joinToString(" ") { "#$it" },
                        color = BrandVioletLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Instagram-style Thin Video Progress Indicator at very bottom (above bottom nav)
        LinearProgressIndicator(
            progress = { playbackProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            color = BrandVioletLight,
            trackColor = Color.White.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun ReelActionButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
