package com.kalsys.inlocker.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulsingSphere(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    minSizeMultiplier: Float = 1f,
    maxSizeMultiplier: Float = 1.1f,
    colorStart: Color = Color.Gray,
    colorEnd: Color = Color.Red,
    durationMillis: Int = 1000,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val sizeAnim by infiniteTransition.animateFloat(
        initialValue = minSizeMultiplier,
        targetValue = maxSizeMultiplier,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val colorAnim by infiniteTransition.animateColor(
        initialValue = colorStart,
        targetValue = colorEnd,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier
        .size(size * sizeAnim)
        .blur(10.dp,  BlurredEdgeTreatment.Unbounded)
    ) {
        drawCircle(color = colorAnim, radius = size.toPx() * sizeAnim / 2)
    }
}

