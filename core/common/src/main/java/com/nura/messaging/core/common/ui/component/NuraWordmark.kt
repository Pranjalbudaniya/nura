package com.nura.messaging.core.common.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily

@Composable
fun NuraWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp
) {
    val brandTextColor = NuraTheme.colors.brandLogoText
    val accentColor = NuraTheme.colors.terracottaAccent

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "nura",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = (-0.04).sp,
            color = brandTextColor
        )
        Canvas(
            modifier = Modifier
                .size((fontSize.value * 0.28).dp)
                .offset(x = 2.dp, y = -(fontSize.value * 0.25).dp)
        ) {
            drawCircle(color = accentColor)
        }
    }
}
