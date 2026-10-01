package com.nura.messaging.features.auth.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.state.PasswordStrengthLevel

@Composable
fun PasswordStrengthBar(
    strength: PasswordStrengthLevel,
    modifier: Modifier = Modifier
) {
    if (strength == PasswordStrengthLevel.EMPTY) return

    val colors = NuraTheme.colors

    // Red for weak pass, Yellow for mid pass, Green for strong pass
    val (activeCount, strengthColor, label) = when (strength) {
        PasswordStrengthLevel.EMPTY -> Triple(0, colors.inputPlaceholder, "")
        PasswordStrengthLevel.WEAK -> Triple(1, colors.strengthWeak, "Weak")
        PasswordStrengthLevel.GOOD -> Triple(2, colors.strengthMedium, "Medium")
        PasswordStrengthLevel.STRONG -> Triple(3, colors.strengthStrong, "Strong")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        // 3 horizontal progress bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..3) {
                val isActive = i <= activeCount
                val targetColor = if (isActive) strengthColor else colors.strengthBarInactive
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 300),
                    label = "strength_bar_$i"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(animatedColor, shape = RoundedCornerShape(9999.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Requirement text on left and strength indicator dot + label on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "8+ characters, letters & numbers",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.04.sp,
                color = colors.subtitleText
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(strengthColor, shape = CircleShape)
                )
                Text(
                    text = label,
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = strengthColor
                )
            }
        }
    }
}
