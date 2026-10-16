package com.dramflix.android.ui.util

fun formatCount(value: Int): String {
    return when {
        value >= 1_000_000 -> {
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
                maximumFractionDigits = 1
            }
            "${formatter.format(value / 1_000_000.0)}M"
        }
        value >= 1_000 -> {
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
                maximumFractionDigits = 1
            }
            "${formatter.format(value / 1_000.0)}K"
        }
        else -> java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value)
    }
}
