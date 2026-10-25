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

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import com.dramflix.android.ui.theme.BrandVioletDark
import com.dramflix.android.ui.theme.DarkBorder

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
    val isReels = currentRoute == Routes.ReelsList

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = if (isReels) Color.Black.copy(alpha = 0.85f) else DarkSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        HorizontalDivider(
                            color = DarkBorder,
                            thickness = 1.dp,
                        )
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                        ) {
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
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BrandVioletLight,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = TextSecondaryDark,
                                        unselectedTextColor = TextSecondaryDark,
                                        indicatorColor = BrandVioletDark.copy(alpha = 0.4f),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
            // --- REMOVE status bar appearance modifiers from Scaffold ---
            // android:statusBarColor, android:navigationBarColor, windowLightStatusBar, windowLightNavigationBar removed
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (isReels) 0.dp else innerPadding.calculateBottomPadding(),
                    ),
            ) {
                content()
            }
        }
}
