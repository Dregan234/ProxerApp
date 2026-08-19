package me.proxer.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProxerColors = darkColorScheme(
    primary = Color(0xFFE85555),
    onPrimary = Color.White,
    secondary = Color(0xFFFFB4AB),
    background = Color(0xFF101010),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF292929),
    onSurface = Color(0xFFF2F2F2)
)

@Composable
fun ProxerTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ProxerColors,
        content = content
    )
}
