package com.dramflix.android.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dramflix.android.navigation.Routes
import com.dramflix.android.ui.theme.BrandVioletLight
import com.dramflix.android.ui.theme.DarkBackground
import com.dramflix.android.ui.theme.DarkSurface
import com.dramflix.android.ui.theme.TextPrimaryDark
import com.dramflix.android.ui.theme.TextSecondaryDark

@Composable
fun AppShell(
    navController: NavController,
    content: @Composable () -> Unit,
) {
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route.orEmpty()
    val showBottomBar = currentRoute in setOf(Routes.Catalog, Routes.ReelsList, Routes.Settings)
    val activeRoot = when {
        currentRoute == Routes.Catalog || currentRoute.startsWith("drama/") || currentRoute.startsWith("player/") -> Tab.Dramas
        currentRoute == Routes.ReelsList || currentRoute.startsWith("reel/") -> Tab.Reels
        else -> Tab.Settings
    }

    // Use contentWindowInsets = WindowInsets(0) so child Scaffolds (on screens with TopAppBar)
    // manage their own top/status bar insets. The AppShell Scaffold only manages the
    // bottom nav bar inset via its bottomBar slot.
    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NavigationBar(containerColor = DarkSurface) {
                        Tab.entries.forEach { tab ->
                            val selected = activeRoot == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(Routes.topLevelDestination(tab)) {
                                        popUpTo(Routes.Catalog) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (selected) BrandVioletLight else TextSecondaryDark,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        color = if (selected) TextPrimaryDark else TextSecondaryDark,
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
