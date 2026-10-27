package com.dramflix.android.ui.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.auth.GoogleIdTokenResult
import com.dramflix.android.data.auth.GoogleSignIn
import com.dramflix.android.data.auth.LinkResult
import com.dramflix.android.ui.theme.BrandRose
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The "Continue with Google" button.
 *
 * Google's branding rules require their official "G" logo, which isn't
 * bundled here. Download it from Google's Sign-In branding guidelines page,
 * add it as `res/drawable/ic_google_logo` (vector or PNG), and it appears
 * automatically; until then a neutral account icon is shown.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val logoRes = remember {
        context.resources.getIdentifier("ic_google_logo", "drawable", context.packageName)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White.copy(alpha = 0.7f),
            disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.7f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1F1F1F),
                    )
                    logoRes != 0 -> Image(
                        painter = painterResource(logoRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF5F6368),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (loading) "Signing in…" else text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Drives "Back up with Google" (link-google) from any screen, including the
 * case where that Google account already owns an app account
 * (`GOOGLE_ACCOUNT_IN_USE` → offer to switch → login-google with the same token).
 */
@Stable
class GoogleBackupLauncher internal constructor(
    private val scope: CoroutineScope,
    private val requestToken: suspend () -> GoogleIdTokenResult,
    private val onMessage: (String) -> Unit,
    private val onLinked: () -> Unit,
) {
    var busy by mutableStateOf(false)
        private set

    /** ID token waiting for the user's answer to "switch to that account?". */
    internal var pendingSwitchToken by mutableStateOf<String?>(null)

    fun launch() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                when (val token = requestToken()) {
                    GoogleIdTokenResult.Cancelled -> Unit
                    is GoogleIdTokenResult.Failure -> onMessage(token.message)
                    is GoogleIdTokenResult.Success -> when (val result = AppContainer.auth.linkGoogle(token.idToken)) {
                        is LinkResult.Linked -> {
                            onMessage("Account backed up to ${result.email ?: token.email ?: "your Google account"}")
                            onLinked()
                        }
                        LinkResult.GoogleAccountInUse -> pendingSwitchToken = token.idToken
                        is LinkResult.AlreadyLinkedToAnother -> onMessage(result.message)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onMessage(e.message ?: "Couldn't link your Google account. Please try again.")
            } finally {
                busy = false
            }
        }
    }

    internal fun confirmSwitch() {
        val idToken = pendingSwitchToken ?: return
        pendingSwitchToken = null
        scope.launch {
            busy = true
            try {
                AppContainer.auth.signInWithGoogle(idToken)
                onMessage("Switched to your Google account")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onMessage(e.message ?: "Couldn't switch accounts. Please try again.")
            } finally {
                busy = false
            }
        }
    }

    internal fun cancelSwitch() {
        pendingSwitchToken = null
    }
}

/**
 * Remembers a [GoogleBackupLauncher] and draws its confirmation dialog.
 * Call from inside a screen; call `launch()` from a button.
 */
@Composable
fun rememberGoogleBackupLauncher(
    onMessage: (String) -> Unit,
    onLinked: () -> Unit = {},
): GoogleBackupLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = remember(scope) {
        GoogleBackupLauncher(
            scope = scope,
            requestToken = { GoogleSignIn.requestIdToken(context) },
            onMessage = onMessage,
            onLinked = onLinked,
        )
    }

    if (launcher.pendingSwitchToken != null) {
        AlertDialog(
            onDismissRequest = launcher::cancelSwitch,
            containerColor = DarkSurface,
            title = {
                Text("Switch to that account?", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This Google account already has a Short Rewards account. " +
                        "If you switch, the coins on this guest account stay behind.",
                    color = TextSecondaryDark,
                )
            },
            confirmButton = {
                TextButton(onClick = launcher::confirmSwitch) {
                    Text("Switch account", color = BrandRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = launcher::cancelSwitch) {
                    Text("Keep this account", color = TextSecondaryDark)
                }
            },
        )
    }
    return launcher
}
