package com.dramflix.android.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(
    val title: String,
    val icon: ImageVector,
) {
    Dramas(
        title = "Dramas",
        icon = Icons.Default.Home,
    ),
    Reels(
        title = "Reels",
        icon = Icons.Default.PlayArrow,
    ),
    Settings(
        title = "Settings",
        icon = Icons.Default.Settings,
    ),
}
