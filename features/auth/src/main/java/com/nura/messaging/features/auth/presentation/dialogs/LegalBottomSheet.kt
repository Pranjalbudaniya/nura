package com.nura.messaging.features.auth.presentation.dialogs

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.ButtonShape
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily

enum class LegalDocumentType {
    TERMS,
    PRIVACY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalBottomSheet(
    initialDocument: LegalDocumentType = LegalDocumentType.TERMS,
    onDismiss: () -> Unit
) {
    var selectedDocument by remember { mutableStateOf(initialDocument) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colors.dividerColor
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header with tab selector and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegalTabButton(
                        title = "Terms of Service",
                        isSelected = selectedDocument == LegalDocumentType.TERMS,
                        onClick = { selectedDocument = LegalDocumentType.TERMS }
                    )

                    LegalTabButton(
                        title = "Privacy Policy",
                        isSelected = selectedDocument == LegalDocumentType.PRIVACY,
                        onClick = { selectedDocument = LegalDocumentType.PRIVACY }
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.subtitleText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Content
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Crossfade(
                    targetState = selectedDocument,
                    label = "legal_document_content"
                ) { docType ->
                    when (docType) {
                        LegalDocumentType.TERMS -> TermsOfServiceContent()
                        LegalDocumentType.PRIVACY -> PrivacyPolicyContent()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.terracottaAccent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Close",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LegalTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = NuraTheme.colors
    val backgroundColor = if (isSelected) colors.terracottaAccent.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (isSelected) colors.terracottaAccent else colors.subtitleText

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            color = textColor
        )
    }
}

@Composable
private fun TermsOfServiceContent() {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Terms of Service",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colorScheme.onSurface
        )

        Text(
            text = "Effective: September 2026",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.subtitleText
        )

        LegalSection(
            number = "1",
            title = "Welcome to Nura",
            body = "Nura is a tranquil, intentional messaging environment designed to eliminate digital friction and cultivate thoughtful communication. By creating an account or accessing the platform, you agree to comply with and be bound by these Terms of Service."
        )

        LegalSection(
            number = "2",
            title = "Account Registration & Security",
            body = "To access Nura, you must provide valid credentials (such as email or Google sign-in). You are solely responsible for safeguarding your credentials and for all activities conducted under your account. Notify us immediately of any unauthorized use."
        )

        LegalSection(
            number = "3",
            title = "Acceptable Conduct",
            body = "Nura is built around mutual respect and tranquility. You agree not to engage in harassment, spam, unlawful transmissions, transmission of malicious code, impersonation, or activities that compromise platform stability or other users' privacy."
        )

        LegalSection(
            number = "4",
            title = "Content Ownership & License",
            body = "You retain complete ownership of all messages, media, and communication transmitted through Nura. You grant Nura only the limited, technical license necessary to route, store, and display your messages across your authorized devices."
        )

        LegalSection(
            number = "5",
            title = "Service Modifications & Availability",
            body = "We continually refine Nura to ensure optimal performance, security, and calm user experience. While we aim for seamless reliability, services may occasionally experience maintenance windows or updates."
        )

        LegalSection(
            number = "6",
            title = "Termination",
            body = "You may delete your account at any time through application settings. We reserve the right to suspend or terminate accounts that breach these terms or threaten the safety and integrity of the Nura community."
        )

        LegalSection(
            number = "7",
            title = "Limitation of Liability",
            body = "Nura is provided on an 'as-is' and 'as-available' basis without warranties of any kind. To the fullest extent permitted by law, Nura shall not be liable for indirect, punitive, or consequential damages."
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Privacy Policy",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colorScheme.onSurface
        )

        Text(
            text = "Effective: September 2026",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.subtitleText
        )

        LegalSection(
            number = "1",
            title = "Our Privacy Commitment",
            body = "Your privacy is fundamental to Nura's calm architecture. We collect only the minimum data required to facilitate real-time messaging, with zero ad tracking, zero data sales, and zero behavioral harvesting."
        )

        LegalSection(
            number = "2",
            title = "Information We Collect",
            body = "• Account Data: Email address and display name for identification.\n• Messages & Media: Stored securely and synchronized across your sessions.\n• Ephemeral Presence: Real-time indicators (e.g. typing or online status) that are transient and not permanently logged.\n• Diagnostics: Minimal crash logs and technical metrics to maintain stability."
        )

        LegalSection(
            number = "3",
            title = "How Information Is Used",
            body = "We use your data solely to deliver conversations, authenticate sessions, prevent malicious abuse, and support your account. We never sell your personal data or read private message contents for marketing purposes."
        )

        LegalSection(
            number = "4",
            title = "Security & Storage",
            body = "All transmissions are encrypted in transit using industry-standard TLS. Account authentication and database storage are safeguarded with strict access controls and audited infrastructure."
        )

        LegalSection(
            number = "5",
            title = "Your Rights & Data Retention",
            body = "You hold the right to review, update, or permanently delete your account and associated conversations. Upon account deletion, personal records and conversation references are expunged from active databases."
        )

        LegalSection(
            number = "6",
            title = "Contact & Inquiries",
            body = "If you have questions regarding this Privacy Policy or wish to exercise your data rights, please contact our privacy team at privacy@nura.app."
        )
    }
}

@Composable
private fun LegalSection(
    number: String,
    title: String,
    body: String
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$number. $title",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = body,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.subtitleText
        )
    }
}
