package com.dramflix.android.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dramflix.android.data.auth.SignInReason
import com.dramflix.android.ui.theme.BrandGradient
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.BrandViolet
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.CardGlassGradient
import com.dramflix.android.ui.theme.CoinGold
import com.dramflix.android.ui.theme.CoinGoldLight
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkBorder
import com.dramflix.android.ui.theme.ErrorRed
import com.dramflix.android.ui.theme.GoldGradient
import com.dramflix.android.ui.theme.TextMutedDark
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark

/** Screen-level entry point: wires the ViewModel to [SignInContent]. */
@Composable
fun SignInScreen(
    reason: SignInReason,
    viewModel: SignInViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SignInContent(
        reason = reason,
        state = state,
        onGoogle = { viewModel.signInWithGoogle(context) },
        onGuest = viewModel::continueAsGuest,
        onDismissError = viewModel::dismissError,
    )
}

private data class ReasonCopy(
    val title: String,
    val subtitle: String,
    val notice: String?,
    val noticeTone: NoticeTone,
    val guestLabel: String,
)

private enum class NoticeTone { Info, Gold }

private fun copyFor(reason: SignInReason) = when (reason) {
    SignInReason.FirstLaunch -> ReasonCopy(
        title = "Welcome to Short Rewards",
        subtitle = "Watch short dramas, earn coins and cash out.",
        notice = null,
        noticeTone = NoticeTone.Info,
        guestLabel = "Continue as guest",
    )
    SignInReason.Returning -> ReasonCopy(
        title = "Welcome back",
        subtitle = "Pick up where you left off.",
        notice = "Sign in with Google to restore your coins, or start fresh as a guest.",
        noticeTone = NoticeTone.Info,
        guestLabel = "Start as a new guest",
    )
    SignInReason.AccountClaimed -> ReasonCopy(
        title = "Recover your account",
        subtitle = "Your coins are safe.",
        notice = "This phone's account is protected with Google. Sign in with the same Google account to get your coins back.",
        noticeTone = NoticeTone.Gold,
        guestLabel = "Start a new guest account",
    )
    SignInReason.SessionExpired -> ReasonCopy(
        title = "Please sign in again",
        subtitle = "Your session has ended.",
        notice = "If you backed up with Google, your coins are waiting. Sign in with the same account.",
        noticeTone = NoticeTone.Info,
        guestLabel = "Start a new guest account",
    )
}

@Composable
fun SignInContent(
    reason: SignInReason,
    state: SignInUiState,
    onGoogle: () -> Unit,
    onGuest: () -> Unit,
    onDismissError: () -> Unit,
) {
    val copy = copyFor(reason)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val busy = state.inProgress != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind {
                // Two soft brand-coloured glows behind the content.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BrandViolet.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.width * 0.9f,
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BrandRose.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.55f),
                        radius = size.width * 0.8f,
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.95f, size.height * 0.55f),
                )
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
        // Top group + bottom group, pushed apart on tall screens, scrollable on short ones.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = copy.title,
                        color = TextPrimaryDark,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = copy.subtitle,
                        color = TextSecondaryDark,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            if (copy.notice != null) {
                Notice(text = copy.notice, tone = copy.noticeTone)
                Spacer(Modifier.height(16.dp))
            }

            BenefitsCard()
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ErrorBanner(
                    message = state.error.orEmpty(),
                    onDismiss = onDismissError,
                )
            }

            GoogleSignInButton(
                text = "Continue with Google",
                onClick = onGoogle,
                loading = state.inProgress == SignInAction.Google,
                enabled = !busy,
                modifier = Modifier.widthIn(max = 480.dp),
            )

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onGuest,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(52.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.inProgress == SignInAction.Guest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = BrandVioletLight,
                    )
                } else {
                    Text(
                        text = copy.guestLabel,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Guests can back up with Google anytime in Settings.",
                color = TextMutedDark,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "  We only use your Google name and email to protect your account.",
                    color = TextMutedDark,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        }
        }
    }
}

/** App mark: play button tile with a coin badge. */
@Composable
private fun BrandMark() {
    Box(modifier = Modifier.size(96.dp)) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(BrandGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkBackground)
                .padding(3.dp)
                .clip(CircleShape)
                .background(GoldGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = Color(0xFF3B2807),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun BenefitsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGlassGradient)
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        BenefitRow(
            icon = Icons.Filled.Shield,
            tint = BrandVioletLight,
            title = "Your coins stay safe",
            body = "Backed up to your Google account, not just this phone.",
        )
        BenefitRow(
            icon = Icons.Filled.PhoneAndroid,
            tint = BrandRose,
            title = "Switch phones anytime",
            body = "Reinstall or get a new phone and sign in to continue.",
        )
        BenefitRow(
            icon = Icons.Filled.AccountBalanceWallet,
            tint = CoinGold,
            title = "Cash out with confidence",
            body = "Redeem to Easypaisa, JazzCash or your bank.",
        )
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = body, color = TextSecondaryDark, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun Notice(text: String, tone: NoticeTone) {
    val accent = if (tone == NoticeTone.Gold) CoinGoldLight else BrandVioletLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = TextPrimaryDark,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            color = TextPrimaryDark,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        TextButton(onClick = onDismiss) {
            Text("OK", color = ErrorRed, fontWeight = FontWeight.Bold)
        }
    }
}

/** Shown while the app checks the saved session on start-up. */
@Composable
fun AuthLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = BrandVioletLight,
            )
        }
    }
}

/** Start-up couldn't reach the server (e.g. offline right after an update). */
@Composable
fun AuthStartupErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Can't connect right now",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = TextSecondaryDark,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = onRetry,
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, BrandVioletLight.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Try again", color = BrandVioletLight, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Android Studio previews ────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Sign in – first launch",
    showBackground = true,
    backgroundColor = 0xFF0B0E14,
    device = "id:pixel_7",
)
@Composable
private fun SignInFirstLaunchPreview() {
    com.dramflix.android.ui.theme.DramaFlixTheme {
        SignInContent(
            reason = SignInReason.FirstLaunch,
            state = SignInUiState(),
            onGoogle = {},
            onGuest = {},
            onDismissError = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Sign in – recover account, error",
    showBackground = true,
    backgroundColor = 0xFF0B0E14,
    device = "id:pixel_7",
)
@Composable
private fun SignInRecoverPreview() {
    com.dramflix.android.ui.theme.DramaFlixTheme {
        SignInContent(
            reason = SignInReason.AccountClaimed,
            state = SignInUiState(error = "No internet connection. Check your network and try again."),
            onGoogle = {},
            onGuest = {},
            onDismissError = {},
        )
    }
}
