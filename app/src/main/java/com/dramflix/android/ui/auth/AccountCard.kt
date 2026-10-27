package com.dramflix.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramflix.android.data.model.UserProfile
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.CoinGoldLight
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.DarkSurfaceCard
import com.dramflix.android.ui.theme.ErrorRed
import com.dramflix.android.ui.theme.SuccessGreen
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import com.dramflix.android.ui.util.formatCount

/**
 * Account security card for Settings.
 * - Guest: prominent "Back up your account" call to action.
 * - Linked: shows the Google email, with sign-out.
 */
@Composable
fun AccountSecurityCard(
    user: UserProfile?,
    onMessage: (String) -> Unit,
    onLinked: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linked = user?.googleLinked == true
    val backup = rememberGoogleBackupLauncher(onMessage = onMessage, onLinked = onLinked)
    var confirmSignOut by remember { mutableStateOf(false) }

    val borderBrush = if (linked) {
        Brush.linearGradient(listOf(DarkBorder, DarkBorder))
    } else {
        Brush.linearGradient(listOf(BrandViolet.copy(alpha = 0.8f), BrandRose.copy(alpha = 0.8f)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background((if (linked) SuccessGreen else CoinGoldLight).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (linked) Icons.Filled.CloudDone else Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (linked) SuccessGreen else CoinGoldLight,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = if (linked) "Backed up with Google" else "Back up your account",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = if (linked) {
                        user?.googleEmail ?: "Google account linked"
                    } else {
                        "You're on a guest account. Link Google so you never lose your coins."
                    },
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!linked) {
            GoogleSignInButton(
                text = "Link Google account",
                onClick = backup::launch,
                loading = backup.busy,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ID: ${user?.userId ?: "—"}",
                color = TextMutedDark,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { confirmSignOut = true }, enabled = !backup.busy) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(16.dp),
                )
                Text("  Sign out", color = TextSecondaryDark, fontSize = 13.sp)
            }
        }
    }

    if (confirmSignOut) {
        val coins = user?.coins ?: 0
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor = DarkSurface,
            icon = if (linked) null else {
                { Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = CoinGoldLight) }
            },
            title = {
                Text(
                    text = if (linked) "Sign out?" else "Sign out of guest account?",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = if (linked) {
                        "You can sign back in with ${user?.googleEmail ?: "your Google account"} anytime. Your coins stay on your account."
                    } else {
                        "This account isn't backed up. If you sign out, its ${formatCount(coins)} coins can't be recovered. Back it up with Google first to keep them."
                    },
                    color = TextSecondaryDark,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) {
                    Text(
                        text = if (linked) "Sign out" else "Sign out anyway",
                        color = if (linked) BrandVioletLight else ErrorRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                if (!linked) {
                    TextButton(onClick = {
                        confirmSignOut = false
                        backup.launch()
                    }) {
                        Text("Back up first", color = BrandVioletLight)
                    }
                } else {
                    TextButton(onClick = { confirmSignOut = false }) {
                        Text("Cancel", color = TextSecondaryDark)
                    }
                }
            },
        )
    }
}
