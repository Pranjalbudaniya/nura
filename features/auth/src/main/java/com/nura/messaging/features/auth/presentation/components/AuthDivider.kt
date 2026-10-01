package com.nura.messaging.features.auth.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.NuraTheme

@Composable
fun AuthDivider(
    text: String = "or continue with",
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val spacing = NuraTheme.spacing
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.dividerColor,
            thickness = 1.dp
        )
        Text(
            text = text,
            style = typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = spacing.medium)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.dividerColor,
            thickness = 1.dp
        )
    }
}
