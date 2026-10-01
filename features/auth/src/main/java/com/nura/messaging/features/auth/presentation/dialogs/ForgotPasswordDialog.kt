package com.nura.messaging.features.auth.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.NuraShapes
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.features.auth.presentation.components.NuraInputField
import com.nura.messaging.features.auth.presentation.components.NuraPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    isSuccess: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = NuraTheme.spacing
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surface, shape = NuraShapes.large)
                .padding(spacing.screenPadding)
        ) {
            Text(
                text = "Reset Password",
                style = typography.titleLarge,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(spacing.small))

            if (isSuccess) {
                Text(
                    text = "A password reset link has been sent to your email. Please follow the instructions in the email to set a new password.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.large))
                NuraPrimaryButton(
                    text = "Close",
                    onClick = onDismiss
                )
            } else {
                Text(
                    text = "Enter your registered email address and we'll send you a link to reset your password.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                NuraInputField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email ID",
                    placeholder = "name@domain.com",
                    leadingIcon = Icons.Outlined.Email,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(spacing.large))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Cancel",
                            style = typography.labelLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(spacing.small))

                    NuraPrimaryButton(
                        text = "Send Link",
                        onClick = onSubmit,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
