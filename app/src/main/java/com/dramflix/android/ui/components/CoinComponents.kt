package com.dramflix.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dramflix.android.data.model.CoinTransaction
import com.dramflix.android.data.model.StreakDayInfo
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.CoinGold
import com.dramflix.android.ui.theme.CoinGoldDark
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CoinTopPill(
    coins: Int,
    streakDay: Int,
    canCheckInToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canCheckInToday) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_scale"
    )

    Row(
        modifier = modifier
            .scale(if (canCheckInToday) scale else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF261D08),
                        Color(0xFF3B2807)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = if (canCheckInToday) GoldGradient else Brush.linearGradient(listOf(DarkBorder, DarkBorder)),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            tint = CoinGold,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "$coins",
            color = CoinGoldLight,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (streakDay > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4C1D95).copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Streak",
                        tint = BrandRose,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "D$streakDay",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (canCheckInToday) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandRose)
            )
        }
    }
}

@Composable
fun RewardsCenterDialog(
    user: UserProfile?,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: () -> Unit,
    onWatchAd: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CoinGold, CoinGoldDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFF1E1500),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Rewards & Streak Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Current Balance: ${user?.coins ?: 0} Coins",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoinGoldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    // 7-Day Streak Roadmap Card
                    item {
                        StreakRoadmapCard(
                            user = user,
                            isSyncing = isSyncing,
                            onCheckIn = onCheckIn
                        )
                    }

                    // Earn More Coins Tasks
                    item {
                        Text(
                            text = "Earn More Coins",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    item {
                        EarnTaskItem(
                            icon = Icons.Default.PlayArrow,
                            iconBg = BrandViolet,
                            title = "Watch Full Episodes",
                            subtitle = "Stream entire episode without skipping",
                            rewardText = "+15 Coins / Ep",
                            buttonText = "Watch",
                            onClick = onDismiss
                        )
                    }

                    item {
                        EarnTaskItem(
                            icon = Icons.Default.Tv,
                            iconBg = BrandRose,
                            title = "Watch Sponsored Video Ad",
                            subtitle = "Short video ad from our sponsors",
                            rewardText = "+25 Coins",
                            buttonText = "Watch Ad",
                            onClick = onWatchAd
                        )
                    }

                    // Transaction History
                    item {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (user?.transactions.isNullOrEmpty()) {
                        item {
                            Text(
                                text = "No coin transactions yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedDark,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(user?.transactions.orEmpty().take(8)) { tx ->
                            TransactionRowItem(tx = tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakRoadmapCard(
    user: UserProfile?,
    isSyncing: Boolean,
    onCheckIn: () -> Unit,
) {
    val canClaim = user?.canCheckInToday == true
    val currentStreak = user?.streakDay ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, if (canClaim) DarkBorder else DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = BrandRose,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "7-Day Daily Streak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = if (currentStreak > 0) "Current streak: Day $currentStreak of 7" else "Start your 7-day streak today!",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF312E81).copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Resets on Day 7",
                    fontSize = 11.sp,
                    color = BrandVioletLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 7 Day Grid Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val schedule = user?.streakSchedule.orEmpty()
            for (day in 1..7) {
                val dayInfo = schedule.find { it.day == day }
                    ?: StreakDayInfo(
                        day = day,
                        coins = when (day) {
                            1 -> 10; 2 -> 20; 3 -> 30; 4 -> 50; 5 -> 70; 6 -> 100; else -> 200
                        },
                        status = if (day <= currentStreak && !canClaim) "claimed" else if (day == 1 && canClaim && currentStreak == 0) "available" else "locked"
                    )

                StreakDayCardItem(
                    dayInfo = dayInfo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Button
        Button(
            onClick = onCheckIn,
            enabled = canClaim && !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canClaim) BrandViolet else DarkSurfaceVariant,
                disabledContainerColor = DarkSurfaceVariant
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (canClaim) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Claim Today's Reward (+${user?.nextCheckInReward ?: 10} Coins)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Claimed for today! Next check-in tomorrow",
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakDayCardItem(
    dayInfo: StreakDayInfo,
    modifier: Modifier = Modifier,
) {
    val isGrandPrize = dayInfo.day == 7
    val isClaimed = dayInfo.status == "claimed"
    val isAvailable = dayInfo.status == "available"

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isAvailable -> Color(0xFF37260C)
                    isClaimed -> Color(0xFF0D2818)
                    else -> DarkBackground
                }
            )
            .border(
                width = if (isAvailable) 1.5.dp else 1.dp,
                color = when {
                    isAvailable -> CoinGold
                    isClaimed -> SuccessGreen.copy(alpha = 0.6f)
                    else -> DarkBorder
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "D${dayInfo.day}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAvailable) CoinGoldLight else TextSecondaryDark
        )

        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isClaimed -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Claimed",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                isAvailable -> {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Available",
                        tint = CoinGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = TextMutedDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = "+${dayInfo.coins}",
            fontSize = if (isGrandPrize) 10.sp else 9.sp,
            fontWeight = if (isGrandPrize) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isGrandPrize) BrandRose else if (isAvailable) CoinGold else TextMutedDark
        )
    }
}

@Composable
private fun EarnTaskItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    rewardText: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = rewardText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoinGoldLight
                )
            }
        }
    }
}

@Composable
private fun TransactionRowItem(tx: CoinTransaction) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(tx.createdAt) { sdf.format(Date(tx.createdAt)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedDark
            )
        }

        Text(
            text = if (tx.amount >= 0) "+${tx.amount}" else "${tx.amount}",
            color = if (tx.amount >= 0) CoinGold else Color(0xFFEF4444),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RewardAdDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit,
) {
    var timeLeft by remember { mutableIntStateOf(5) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        isFinished = true
    }

    Dialog(
        onDismissRequest = {
            if (isFinished) onDismiss()
        },
        properties = DialogProperties(dismissOnClickOutside = isFinished, dismissOnBackPress = isFinished)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldGradient, RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ad Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SPONSORED AD", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (isFinished) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    } else {
                        Text("Reward in ${timeLeft}s", fontSize = 12.sp, color = CoinGold, fontWeight = FontWeight.Bold)
                    }
                }

                // Ad Video Simulation Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF020617)))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = if (isFinished) SuccessGreen else BrandRose,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (isFinished) "🎉 Ad Watched Completely!" else "Playing Sponsored Promo Video...",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isFinished) 1f else (5 - timeLeft) / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CoinGold,
                    trackColor = Color(0xFF334155),
                )

                if (isFinished) {
                    Button(
                        onClick = {
                            onRewardEarned()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                    ) {
                        Text(
                            text = "Claim +25 Coins Reward 🪙",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Watch till the end to unlock your 25 Coins reward!",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeCompletedCelebrationDialog(
    coinsEarned: Int = 15,
    nextEpisode: Int?,
    onPlayNext: () -> Unit,
    onWatchAdForDouble: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, GoldGradient, RoundedCornerShape(24.dp)),
            color = Color(0xFF111827)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CoinGold, CoinGoldDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFF1E1500),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Episode Complete!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+$coinsEarned Coins Added to your Wallet 🪙",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CoinGoldLight
                    )
                }

                HorizontalDivider(color = DarkBorder)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (nextEpisode != null) {
                        Button(
                            onClick = onPlayNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                        ) {
                            Text(
                                text = "Play Episode $nextEpisode ▶",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = onWatchAdForDouble,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37260C))
                    ) {
                        Text(
                            text = "📺 Watch Ad for +25 Extra Coins",
                            fontWeight = FontWeight.Bold,
                            color = CoinGoldLight
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = "Back to Drama",
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}
