package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.ui.theme.LocalSaathiColors

@Composable
fun SimulateBankSmsPanel(
    onDismiss: () -> Unit,
    onSubmitSms: (sender: String, body: String) -> Unit
) {
    val ext = LocalSaathiColors.current
    var sender by remember { mutableStateOf("AD-HDFCBK") }
    var smsBody by remember {
        val ref = "6300" + (System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')
        mutableStateOf("Sent Rs.450.00 From HDFC Bank A/C *4821 To Swiggy On 27/10/26 Ref $ref")
    }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulate_sms_panel")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Test Bank SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        val ref = "6301" + (System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')
                        sender = "AD-HDFCBK"
                        smsBody = "Sent Rs.450.00 From HDFC Bank A/C *4821 To Swiggy On 27/10/26 Ref $ref"
                    },
                    label = { Text("HDFC ₹450") },
                    modifier = Modifier.testTag("sample_sms_hdfc_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val ref = "6302" + (System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')
                        sender = "VM-SBIUPI"
                        smsBody = "Dear UPI user A/C X9104 debited by 1280.0 on date 27Oct26 trf to BLINKIT GROCERY Refno $ref -SBI"
                    },
                    label = { Text("SBI ₹1,280") },
                    modifier = Modifier.testTag("sample_sms_sbi_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val ref = "6303" + (System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')
                        sender = "JX-ICICIB"
                        smsBody = "ICICI Bank Acct XX7732 debited for Rs 899.00 on 27-Oct-26; AMAZON PAY INDIA credited. UPI:$ref."
                    },
                    label = { Text("ICICI ₹899") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val ref = "6304" + (System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')
                        sender = "AD-HDFCBK"
                        smsBody = "Your HDFC Bank A/c XX4821 is credited with INR 45,000.00 on 01-10-26 by ACME TECH SALARY Ref $ref. Avl bal INR 58,200.00"
                    },
                    label = { Text("Salary ₹45k") }
                )
            }

            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Sender") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_sms_sender_input")
            )

            OutlinedTextField(
                value = smsBody,
                onValueChange = { smsBody = it },
                label = { Text("SMS Text") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_sms_body_input")
            )

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (smsBody.isNotBlank()) {
                            onSubmitSms(sender.trim(), smsBody.trim())
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("simulate_sms_submit_button")
                ) {
                    Text("Detect", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationPreferencesPanel(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (upi: Boolean, budget: Boolean, daily: Boolean, payday: Boolean, smartAiDigest: Boolean) -> Unit
) {
    val ext = LocalSaathiColors.current
    var upi by remember(preferences.notifyUpiApprovals) { mutableStateOf(preferences.notifyUpiApprovals) }
    var budget by remember(preferences.notifyBudgetWarnings) { mutableStateOf(preferences.notifyBudgetWarnings) }
    var daily by remember(preferences.notifyDailyLimit) { mutableStateOf(preferences.notifyDailyLimit) }
    var payday by remember(preferences.notifyPaydayReminders) { mutableStateOf(preferences.notifyPaydayReminders) }
    var smartDigest by remember(preferences.notifySmartAiDigest) { mutableStateOf(preferences.notifySmartAiDigest) }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        borderColor = ext.allowanceBorder,
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_prefs_panel")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Alert Types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close"
                    )
                }
            }

            AlertToggleRow(title = "UPI Approvals", checked = upi, onCheckedChange = { upi = it })
            HorizontalDivider(color = ext.cardBorder)
            AlertToggleRow(title = "Budget Limits (85% / 100%)", checked = budget, onCheckedChange = { budget = it })
            HorizontalDivider(color = ext.cardBorder)
            AlertToggleRow(title = "Daily Limit", checked = daily, onCheckedChange = { daily = it })
            HorizontalDivider(color = ext.cardBorder)
            AlertToggleRow(title = "Payday Reminders", checked = payday, onCheckedChange = { payday = it })
            HorizontalDivider(color = ext.cardBorder)
            AlertToggleRow(
                title = "Smart Spending Insight (Max 1/day)",
                checked = smartDigest,
                onCheckedChange = { smartDigest = it }
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(upi, budget, daily, payday, smartDigest)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AlertToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
