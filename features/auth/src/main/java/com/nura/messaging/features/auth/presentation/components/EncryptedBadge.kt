package com.nura.messaging.features.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily

@Composable
fun EncryptedBadge(
    modifier: Modifier = Modifier,
    text: String = "By joining, you enter an end-to-end encrypted sanctuary built for deliberate presence."
) {
    val colors = NuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.badgeBackground, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = colors.badgeBorder, shape = RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Spa,
            contentDescription = null,
            tint = colors.terracottaAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = colors.subtitleText
        )
    }
}
