package com.kalsys.inlocker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun customButtonColors() = ButtonDefaults.buttonColors(
    containerColor = CustomPrimary,
    contentColor = CustomWhite,
    disabledContainerColor = CustomPrimaryDark,
    disabledContentColor = Color.Gray
)

private val DarkColorScheme = darkColorScheme(
    primary = CustomPrimary,
    onPrimary = CustomWhite,
    secondary = CustomSecondaryDark,
    onSecondary = CustomWhite,
    background = CustomBlack,
    onBackground = CustomWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CustomPrimary,
    onPrimary = CustomWhite,
    secondary = CustomSecondary,
    onSecondary = CustomWhite,
    background = CustomWhite,
    onBackground = CustomBlack
)

@Composable
fun InLockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}