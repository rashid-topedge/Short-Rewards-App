package com.dramflix.android.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dramflix.android.data.model.DramaCard
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
import com.dramflix.android.ui.theme.DarkBorderHighlight
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.DarkSurfaceVariant
import com.dramflix.android.ui.theme.GoldGradient
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount

@Composable
fun HeroFeaturedBanner(
    drama: DramaCard?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drama == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = drama.cover,
            contentDescription = drama.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Multi-stage Dark Gradient for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x80000000),
                            Color(0xEE0B0E14)
                        ),
                        startY = 60f
                    )
                )
        )

        // Badges Top Right
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandRose)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "FEATURED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HD 4K",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Details Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = drama.title ?: "Featured Drama",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${formatCount(drama.playCount)} views",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "${drama.episodeCount} Episodes",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGradient)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Watch Now",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCheckInBanner(
    user: UserProfile?,
    onOpenRewards: () -> Unit,
    onQuickCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canCheckIn = user?.canCheckInToday == true
    val streakDay = user?.streakDay ?: 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (canCheckIn) GoldGradient else Brush.linearGradient(listOf(DarkBorder, DarkBorder)), RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenRewards),
        color = DarkSurfaceCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (canCheckIn) Brush.radialGradient(listOf(CoinGold, CoinGoldDark))
                            else Brush.radialGradient(listOf(BrandViolet, BrandVioletLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (canCheckIn) Icons.Default.Whatshot else Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = if (canCheckIn) Color(0xFF1E1500) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (canCheckIn) "Claim Day ${if (streakDay >= 7) 1 else streakDay + 1} Reward! 🔥" else "Day $streakDay Streak Active! 🎉",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (canCheckIn) "Tap to collect +${user?.nextCheckInReward ?: 10} coins" else "Next reward ready tomorrow (Resets on Day 7)",
                        color = if (canCheckIn) CoinGoldLight else TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            if (canCheckIn) {
                Button(
                    onClick = onQuickCheckIn,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    Text(
                        text = "Claim",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F281E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Done ✓",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GenreChipsRow(
    selectedGenre: String,
    onSelectGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genres = listOf(
        "All",
        "Trending 🔥",
        "Romance 💕",
        "CEO / Billionaire 💼",
        "Revenge ⚔️",
        "Mystery 🔍",
        "Fantasy 🔮",
        "Action ⚡"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(genres) { genre ->
            val isSelected = selectedGenre == genre
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) BrandViolet else DarkSurfaceCard
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) BrandVioletLight else DarkBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectGenre(genre) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = genre,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun ModernDramaGridItem(
    drama: DramaCard,
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = drama.cover,
                contentDescription = drama.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark subtle gradient at bottom of poster
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0B0E14)),
                            startY = 140f
                        )
                    )
            )

            // Episode Badge Top Right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${drama.episodeCount} EPS",
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Play Count Bottom Left
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFFF1F5F9),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = formatCount(drama.playCount),
                    color = Color(0xFFF1F5F9),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = drama.title ?: "Untitled Drama",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = drama.latestEpisodeLabel ?: "${drama.episodeCount} episodes total",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
