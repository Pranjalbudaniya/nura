package com.nura.messaging.features.auth.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.ButtonShape
import com.nura.messaging.core.common.ui.theme.NuraShapes
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.features.auth.presentation.components.NuraPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationDialog(
    email: String,
    resendCooldownSeconds: Int,
    isResending: Boolean,
    isResentSuccess: Boolean,
    onResendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = NuraTheme.spacing
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val colors = NuraTheme.colors

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surface, shape = NuraShapes.large)
                .padding(spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.MarkEmailRead,
                contentDescription = null,
                tint = colors.terracottaAccent,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = "Verify your email",
                style = typography.titleLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Text(
                text = "We sent a confirmation link to $email. Please check your inbox and verify your account to start messaging.",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isResentSuccess) {
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = "A new verification link has been sent.",
                    style = typography.labelSmall,
                    color = colors.terracottaAccent,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))

            val resendText = if (resendCooldownSeconds > 0) {
                "Resend in ${resendCooldownSeconds}s"
            } else if (isResending) {
                "Sending..."
            } else {
                "Resend email"
            }

            OutlinedButton(
                onClick = onResendClick,
                enabled = resendCooldownSeconds == 0 && !isResending,
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.oAuthButtonHeight)
            ) {
                Text(
                    text = resendText,
                    style = typography.labelLarge,
                    color = if (resendCooldownSeconds == 0 && !isResending) colorScheme.primary else colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))

            NuraPrimaryButton(
                text = "Got it",
                onClick = onDismiss
            )
        }
    }
}
