package com.kalsys.inlocker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.ui.theme.customButtonColors

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)

) {
    Button(
        onClick = onClick,
//        colors =  customButtonColors(),
        modifier = modifier,
        shape = shape
    ) {
        Text(text)
    }
}
