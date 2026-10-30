package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.UserPreferences
import com.example.ui.components.AiSaathiBrandAvatar
import com.example.ui.components.RichAssistantCard
import com.example.ui.components.SaathiCard
import com.example.ui.components.WaveTypingBubble
import com.example.ui.components.rememberReducedMotion
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import com.example.viewmodel.AiChatUiState

private enum class AiTopMode {
    CHAT,
    HISTORY
}

private val QuickPrompts = listOf(
    "Groceries ₹100 add karo",
    "Aaj safe spend kitna hai?",
    "Plan my budget",
    "Top kharche batao"
)

@Composable
fun AiAssistantScreen(
    preferences: UserPreferences,
    sessions: List<ChatSessionEntity>,
    activeSessionId: Long?,
    messages: List<ChatMessageEntity>,
    chatUiState: AiChatUiState,
    summary: MonthlyFinancialSummary? = null,
    undoableMessageIds: Set<Long> = emptySet(),
    onUndoMessage: (Long) -> Unit = {},
    onToggleAiOptIn: (Boolean) -> Unit,
    onSelectSession: (Long) -> Unit,
    onStartNewChat: () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryLastMessage: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
    onCopiedFeedback: (String) -> Unit,
    onPreWarmAi: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = LocalSaathiColors.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    var selectedTopMode by remember { mutableStateOf(AiTopMode.CHAT) }
    var inputText by remember { mutableStateOf("") }

    val isBusy = chatUiState is AiChatUiState.Loading || chatUiState is AiChatUiState.Streaming
    val currency = preferences.currencySymbol
    val showDecimals = preferences.showDecimalsInLists

    LaunchedEffect(Unit) {
        onPreWarmAi()
    }

    LaunchedEffect(messages.size, chatUiState, selectedTopMode, isImeVisible) {
        if (selectedTopMode == AiTopMode.CHAT) {
            val extra = when (chatUiState) {
                is AiChatUiState.Loading, is AiChatUiState.Streaming, is AiChatUiState.Error -> 1
                AiChatUiState.Idle -> 0
            }
            val totalItems = (if (messages.isEmpty()) 1 else messages.size) + extra
            if (totalItems > 0 && (messages.isNotEmpty() || isImeVisible)) {
                listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            // 1. Sleek Top Bar: Upgraded AI Saathi 3D Emblem + Dynamic Expanding [Chat | History] Pill + New Chat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AiSaathiBrandAvatar(
                            size = 40.dp,
                            isCircle = true
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (preferences.aiOptInEnabled) ext.incomeGreen else ext.textSecondary)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Saathi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Hinglish • हिंदी • EN",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.accentSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Dynamic Expanding Pill Dock for Chat & History (matches bottom navigation dock!)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = ext.dockSurface,
                    border = BorderStroke(1.dp, ext.dockBorder),
                    shadowElevation = 3.dp,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                    ) {
                        DynamicTopModePill(
                            selected = selectedTopMode == AiTopMode.CHAT,
                            label = "Chat",
                            selectedIcon = Icons.Filled.ChatBubble,
                            unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTopMode = AiTopMode.CHAT
                            },
                            testTag = "ai_tab_chat"
                        )

                        DynamicTopModePill(
                            selected = selectedTopMode == AiTopMode.HISTORY,
                            label = if (sessions.isNotEmpty()) "History (${sessions.size})" else "History",
                            selectedIcon = Icons.Filled.History,
                            unselectedIcon = Icons.Outlined.History,
                            badgeDot = sessions.isNotEmpty() && selectedTopMode != AiTopMode.HISTORY,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTopMode = AiTopMode.HISTORY
                            },
                            testTag = "ai_tab_history"
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    onClick = {
                        onStartNewChat()
                        selectedTopMode = AiTopMode.CHAT
                    },
                    shape = CircleShape,
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("new_ai_chat_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (selectedTopMode == AiTopMode.CHAT) {
                // 2. Auto-Compacting Summary Context Strip (collapses extra pills when keyboard opens so chat stays roomy!)
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 16.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isImeVisible) 4.dp else 7.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isImeVisible && preferences.aiOptInEnabled && summary != null) {
                                        "Summary shared • Left ${FinanceUtils.formatRupee(summary.remainingAvailableBalance, showDecimals, currency)}"
                                    } else {
                                        "Share monthly summary"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = preferences.aiOptInEnabled,
                                onCheckedChange = onToggleAiOptIn,
                                modifier = Modifier
                                    .scale(0.82f)
                                    .testTag("chat_ai_opt_in_switch")
                            )
                        }

                        AnimatedVisibility(
                            visible = !isImeVisible && preferences.aiOptInEnabled && summary != null && summary.totalReceivedIncome > 0,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (summary != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 2.dp)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    ContextSnapshotPill(
                                        label = "Income",
                                        value = FinanceUtils.formatRupee(summary.totalReceivedIncome, showDecimals, currency),
                                        accent = ext.incomeGreen
                                    )
                                    ContextSnapshotPill(
                                        label = "Left",
                                        value = FinanceUtils.formatRupee(summary.remainingAvailableBalance, showDecimals, currency),
                                        accent = ext.accentPrimary
                                    )
                                    ContextSnapshotPill(
                                        label = "Today",
                                        value = FinanceUtils.formatRupee(summary.dailyAllowance.suggestedTodayAmount, showDecimals, currency),
                                        accent = ext.accentTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Spacious Messages Feed
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        item {
                            RichAssistantCard(
                                text = "Namaste! Ask in **Hinglish**, **हिंदी**, or **English**—I can instantly **add, edit, or delete** expenses, budgets, goals, and categories (with 1-tap **Undo**), or check your daily safe spend.",
                                onCopy = {
                                    copyToClipboard(context, it)
                                    onCopiedFeedback("Copied.")
                                }
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            if (msg.role == "user") {
                                UserBubble(text = msg.content)
                            } else {
                                RichAssistantCard(
                                    text = msg.content,
                                    canUndo = msg.id in undoableMessageIds,
                                    onUndo = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUndoMessage(msg.id)
                                    },
                                    undoTestTag = "ai_undo_button_${msg.id}",
                                    undoneStatusTestTag = "ai_undone_badge_${msg.id}",
                                    onCopy = { cleanText ->
                                        copyToClipboard(context, cleanText)
                                        onCopiedFeedback("Copied.")
                                    }
                                )
                            }
                        }
                    }

                    when (chatUiState) {
                        is AiChatUiState.Loading -> {
                            item { WaveTypingBubble() }
                        }

                        is AiChatUiState.Streaming -> {
                            item {
                                RichAssistantCard(
                                    text = chatUiState.partialText,
                                    isStreaming = true,
                                    onCopy = {}
                                )
                            }
                        }

                        is AiChatUiState.Error -> {
                            item {
                                SaathiCard(
                                    containerColor = ext.warningContainer,
                                    borderColor = ext.warning.copy(alpha = 0.4f),
                                    cornerRadius = 16.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = "AI Error",
                                            tint = ext.warning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = chatUiState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = onRetryLastMessage,
                                            modifier = Modifier.testTag("retry_ai_chat_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry")
                                        }
                                        TextButton(onClick = onDismissError) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }

                        AiChatUiState.Idle -> Unit
                    }
                }

                // 4. Compact Bilingual Quick Prompts (auto-hide while actively typing text so screen stays clean)
                AnimatedVisibility(visible = inputText.isEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        QuickPrompts.forEach { promptChip ->
                            AssistChip(
                                onClick = {
                                    if (!isBusy) {
                                        onSendMessage(promptChip)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ai_saathi_spark),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = ext.surfaceElevated
                                ),
                                label = {
                                    Text(
                                        text = promptChip,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }

                // 5. Sleek Compact Single-Capsule Composer (zero ugly nested OutlinedTextField border!)
                val canSend = inputText.isNotBlank() && !isBusy
                Surface(
                    color = ext.surfaceElevated,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 4.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (inputText.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        } else {
                            ext.cardBorder
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(start = 16.dp, end = 5.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val trimmed = inputText.trim()
                                    if (trimmed.isNotBlank() && !isBusy) {
                                        onSendMessage(trimmed)
                                        inputText = ""
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = "Ask in Hinglish, हिंदी or English...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ext.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                val trimmed = inputText.trim()
                                if (trimmed.isNotBlank() && !isBusy) {
                                    onSendMessage(trimmed)
                                    inputText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        Brush.linearGradient(
                                            listOf(ext.heroGradientMid, ext.accentPrimary)
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(ext.surfaceSubtle, ext.surfaceSubtle))
                                    }
                                )
                                .testTag("ai_chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else ext.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // Dynamic History View
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("chat_history_list")
                ) {
                    if (sessions.isEmpty()) {
                        item {
                            SaathiCard(
                                containerColor = ext.surfaceElevated,
                                cornerRadius = 22.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No chat history",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            onStartNewChat()
                                            selectedTopMode = AiTopMode.CHAT
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Start Chat")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${sessions.size} saved chats",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ext.textSecondary
                                )
                                TextButton(
                                    onClick = onClearChat,
                                    modifier = Modifier.testTag("clear_ai_chat_button")
                                ) {
                                    Text("Clear All", color = ext.expenseRed)
                                }
                            }
                        }

                        items(sessions, key = { it.id }) { session ->
                            val isActive = session.id == activeSessionId
                            SaathiCard(
                                containerColor = ext.surfaceElevated,
                                borderColor = if (isActive) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                cornerRadius = 18.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSession(session.id)
                                        selectedTopMode = AiTopMode.CHAT
                                    }
                                    .testTag("chat_session_card_${session.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = session.lastMessagePreview.ifBlank { "No messages yet" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${FinanceUtils.formatDate(session.updatedAtMillis, "dd MMM, h:mm a")} • ${session.messageCount} msgs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.textSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_chat_session_${session.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = ext.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicTopModePill(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badgeDot: Boolean = false
) {
    val ext = LocalSaathiColors.current
    val pillBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "ai_top_pill_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else ext.textSecondary,
        label = "ai_top_pill_tint"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = 44.dp, minHeight = 42.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(pillBg)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 12.dp else 10.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Box {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            if (badgeDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ext.accentTertiary)
                )
            }
        }

        AnimatedVisibility(
            visible = selected,
            enter = if (reducedMotion) {
                fadeIn()
            } else {
                fadeIn() + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exit = if (reducedMotion) {
                fadeOut()
            } else {
                fadeOut() + shrinkHorizontally()
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContextSnapshotPill(
    label: String,
    value: String,
    accent: Color
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ext.surfaceSubtle)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = ext.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val ext = LocalSaathiColors.current
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                .background(
                    Brush.linearGradient(
                        listOf(ext.heroGradientStart, ext.heroGradientMid)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Salary Saathi Advice", text))
}
