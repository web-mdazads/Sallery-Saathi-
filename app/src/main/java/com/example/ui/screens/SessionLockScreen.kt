package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.ui.components.SaathiCard
import com.example.ui.components.SaathiUserAvatar
import com.example.ui.components.rememberReducedMotion
import com.example.ui.theme.LocalSaathiColors
import com.example.viewmodel.SaathiViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Non-destructive Session Lock / Auto-Lock screen shown on cold start, background resume
 * (when a 4-digit PIN is active), or manual Log Out in Settings.
 * All Room transactions, budgets, categories, goals, and preferences stay 100% preserved.
 */
@Composable
fun SessionLockScreen(
    preferences: UserPreferences,
    onUnlockWithPin: (String) -> Boolean,
    onSetPinCode: (pinCode: String, recoveryQuestion: String, recoveryAnswer: String) -> Unit,
    onResetPinWithRecovery: (enteredAnswer: String, newPinCode: String) -> Boolean
) {
    val context = LocalContext.current
    val ext = LocalSaathiColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val reducedMotion = rememberReducedMotion()
    val hasPin = preferences.appPinCode.length == 4
    var enteredPin by remember { mutableStateOf("") }
    var isVerifyingPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreatePinPanel by remember { mutableStateOf(false) }
    var showForgotPinPanel by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val fourthDotBeat = remember { Animatable(0f) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(preferences.pinLockoutUntilMillis) {
        nowMillis = System.currentTimeMillis()
        while (preferences.pinLockoutUntilMillis > nowMillis) {
            delay(250L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val remainingLockSeconds = if (preferences.pinLockoutUntilMillis > nowMillis) {
        ((preferences.pinLockoutUntilMillis - nowMillis + 999L) / 1000L).toInt().coerceAtLeast(1)
    } else {
        0
    }
    val isLockedOut = remainingLockSeconds > 0

    // Prevent hardware/gesture Back from bypassing the lock screen; close inline panels first
    BackHandler(enabled = true) {
        when {
            showForgotPinPanel -> showForgotPinPanel = false
            showCreatePinPanel -> showCreatePinPanel = false
            else -> (context as? Activity)?.moveTaskToBack(true)
        }
    }

    val handleDigitTap: (String) -> Unit = tap@{ digit ->
        if (isLockedOut || isVerifyingPin) return@tap
        if (enteredPin.length < 4) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val next = enteredPin + digit
            enteredPin = next
            errorMessage = null
            if (next.length == 4) {
                isVerifyingPin = true
                val priorAttempts = if (preferences.failedPinAttempts >= SaathiViewModel.MAX_PIN_ATTEMPTS) {
                    0
                } else {
                    preferences.failedPinAttempts
                }
                scope.launch {
                    // Hold briefly on Compose frame clock so all 4 filled dots (● ● ● ●) visibly paint before unlock
                    fourthDotBeat.snapTo(0f)
                    fourthDotBeat.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = if (reducedMotion) 60 else 170)
                    )
                    fourthDotBeat.snapTo(0f)

                    val ok = onUnlockWithPin(next)
                    if (!ok) {
                        nowMillis = System.currentTimeMillis()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val newAttempts = priorAttempts + 1
                        errorMessage = if (newAttempts >= SaathiViewModel.MAX_PIN_ATTEMPTS) {
                            null
                        } else {
                            val remaining = SaathiViewModel.MAX_PIN_ATTEMPTS - newAttempts
                            "Incorrect PIN. $remaining ${if (remaining == 1) "attempt" else "attempts"} left."
                        }
                        enteredPin = ""
                        isVerifyingPin = false
                        listOf(-18f, 18f, -12f, 12f, 0f).forEach { target ->
                            shakeOffset.animateTo(target, tween(55))
                        }
                    } else {
                        isVerifyingPin = false
                    }
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxSize()
            .testTag("session_lock_screen")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .testTag("session_lock_scroll")
            ) {
                // Avatar + Tonal Main Halo
                SaathiUserAvatar(
                    avatarKey = preferences.avatarKey,
                    userName = preferences.userName,
                    size = 84.dp,
                    showRing = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (preferences.userName.isNotBlank()) {
                        "Welcome back, ${preferences.userName}"
                    } else {
                        "Welcome back"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("session_lock_welcome_title")
                )

            if (preferences.userRole.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preferences.userRole,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reassurance badge: Data is safely preserved (Accent 1 Emerald)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ext.accentSecondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = ext.accentSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Session locked • All saved data intact",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ext.accentSecondary
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            when {
                showForgotPinPanel -> {
                    InlineRecoverPinCard(
                        preferences = preferences,
                        onCancel = { showForgotPinPanel = false },
                        onResetPinWithRecovery = { answer, newPin ->
                            val resetOk = onResetPinWithRecovery(answer, newPin)
                            if (resetOk) {
                                showForgotPinPanel = false
                            }
                            resetOk
                        }
                    )
                }

                showCreatePinPanel -> {
                    InlineCreatePinCard(
                        onCancel = { showCreatePinPanel = false },
                        onSaveAndUnlock = { newPin, question, answer ->
                            showCreatePinPanel = false
                            onSetPinCode(newPin, question, answer)
                            onUnlockWithPin(newPin)
                        }
                    )
                }

                hasPin -> {
                    Text(
                        text = if (isLockedOut) {
                            "PIN keypad paused for security"
                        } else {
                            "Enter your 4-digit PIN"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Dot PIN Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .testTag("pin_dots_row")
                    ) {
                        repeat(4) { index ->
                            val filled = index < enteredPin.length
                            val dotScale = if (index == 3 && filled && isVerifyingPin && !reducedMotion) {
                                1f + 0.18f * sin(fourthDotBeat.value * PI).toFloat()
                            } else {
                                1f
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        scaleX = dotScale
                                        scaleY = dotScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (filled) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                        shape = CircleShape
                                    )
                                    .semantics {
                                        stateDescription = if (filled) "Filled" else "Empty"
                                    }
                                    .testTag("pin_dot_$index")
                            )
                        }
                    }

                    AnimatedVisibility(visible = isLockedOut) {
                        SaathiCard(
                            containerColor = ext.dangerContainer,
                            borderColor = ext.danger.copy(alpha = 0.45f),
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("pin_lockout_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LockClock,
                                    contentDescription = null,
                                    tint = ext.danger,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Too many wrong attempts • Try again in ${remainingLockSeconds}s",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.danger
                                    )
                                    Text(
                                        text = "Or use Recover PIN Safely below with your Security Answer.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !isLockedOut && errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.expenseRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("pin_error_message")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3x4 Tactile Keypad
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(if (isLockedOut) 0.45f else 1f)
                    ) {
                        rows.forEach { rowDigits ->
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                rowDigits.forEach { digit ->
                                    PinKeypadButton(
                                        label = digit,
                                        enabled = !isLockedOut && !isVerifyingPin,
                                        onClick = { handleDigitTap(digit) },
                                        testTag = "pin_key_$digit"
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Surface(
                                onClick = {
                                    if (!isLockedOut && !isVerifyingPin) {
                                        enteredPin = ""
                                        errorMessage = null
                                    }
                                },
                                enabled = !isLockedOut && !isVerifyingPin,
                                shape = CircleShape,
                                color = ext.surfaceSubtle,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("pin_key_clear")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            PinKeypadButton(
                                label = "0",
                                enabled = !isLockedOut && !isVerifyingPin,
                                onClick = { handleDigitTap("0") },
                                testTag = "pin_key_0"
                            )

                            Surface(
                                onClick = {
                                    if (!isLockedOut && !isVerifyingPin && enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                        errorMessage = null
                                    }
                                },
                                enabled = !isLockedOut && !isVerifyingPin,
                                shape = CircleShape,
                                color = ext.surfaceSubtle,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("pin_key_backspace")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLockedOut) {
                        FilledTonalButton(
                            onClick = { showForgotPinPanel = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("forgot_pin_button")
                        ) {
                            Text(
                                text = "Recover PIN Safely",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { showForgotPinPanel = true },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("forgot_pin_button")
                        ) {
                            Text(
                                text = "Forgot PIN?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> {
                    // No PIN configured yet: 1-tap Sign Back In + Optional PIN Setup
                    SaathiCard(
                        containerColor = ext.surfaceElevated,
                        cornerRadius = 24.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Button(
                                onClick = { onUnlockWithPin("") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("unlock_session_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign Back In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { showCreatePinPanel = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("lock_screen_setup_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Pin,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set Optional 4-Digit PIN")
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
private fun InlineCreatePinCard(
    onCancel: () -> Unit,
    onSaveAndUnlock: (pinCode: String, recoveryQuestion: String, recoveryAnswer: String) -> Unit
) {
    val ext = LocalSaathiColors.current
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryQuestion by remember { mutableStateOf("What is your favourite city?") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Create 4-Digit PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Locks automatically on every app open, background resume & Log Out.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.textSecondary
            )
            OutlinedTextField(
                value = newPin,
                onValueChange = { raw ->
                    newPin = raw.filter { it.isDigit() }.take(4)
                    createError = null
                },
                label = { Text("4-Digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_dialog_pin_input")
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { raw ->
                    confirmPin = raw.filter { it.isDigit() }.take(4)
                    createError = null
                },
                label = { Text("Confirm 4-Digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_dialog_confirm_pin_input")
            )
            OutlinedTextField(
                value = recoveryQuestion,
                onValueChange = {
                    recoveryQuestion = it
                    createError = null
                },
                label = { Text("Recovery Question") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_dialog_recovery_question_input")
            )
            OutlinedTextField(
                value = recoveryAnswer,
                onValueChange = {
                    recoveryAnswer = it
                    createError = null
                },
                label = { Text("Recovery Security Answer") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_dialog_recovery_answer_input")
            )
            if (createError != null) {
                Text(
                    text = createError.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.expenseRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        when {
                            newPin.length != 4 -> createError = "Enter a 4-digit PIN."
                            confirmPin != newPin -> createError = "PINs do not match."
                            recoveryAnswer.trim().isEmpty() -> createError = "Enter a recovery answer so your data stays safe."
                            else -> onSaveAndUnlock(newPin, recoveryQuestion.trim(), recoveryAnswer.trim())
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("lock_dialog_save_pin_button")
                ) {
                    Text("Save PIN")
                }
            }
        }
    }
}

@Composable
private fun InlineRecoverPinCard(
    preferences: UserPreferences,
    onCancel: () -> Unit,
    onResetPinWithRecovery: (enteredAnswer: String, newPinCode: String) -> Boolean
) {
    val ext = LocalSaathiColors.current
    var answerInput by remember { mutableStateOf("") }
    var resetPinInput by remember { mutableStateOf("") }
    var confirmResetPinInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    val hasRecoveryConfigured = preferences.pinRecoveryAnswer.isNotBlank()

    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Recover PIN Safely",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (hasRecoveryConfigured) {
                Text(
                    text = "Answer your security question to set a new 4-digit PIN. All transactions, budgets & profile stay 100% safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.textSecondary
                )
                Text(
                    text = preferences.pinRecoveryQuestion.ifBlank { "Security recovery question" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = {
                        answerInput = it
                        recoveryError = null
                    },
                    label = { Text("Your Security Answer") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_answer_input")
                )
                OutlinedTextField(
                    value = resetPinInput,
                    onValueChange = { raw ->
                        resetPinInput = raw.filter { it.isDigit() }.take(4)
                        recoveryError = null
                    },
                    label = { Text("New 4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_new_pin_input")
                )
                OutlinedTextField(
                    value = confirmResetPinInput,
                    onValueChange = { raw ->
                        confirmResetPinInput = raw.filter { it.isDigit() }.take(4)
                        recoveryError = null
                    },
                    label = { Text("Confirm New 4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_confirm_pin_input")
                )
                if (recoveryError != null) {
                    Text(
                        text = recoveryError.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.expenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("recovery_cancel_button")
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            when {
                                answerInput.trim().isEmpty() -> recoveryError = "Enter your recovery answer."
                                resetPinInput.length != 4 -> recoveryError = "Enter a new 4-digit PIN."
                                confirmResetPinInput != resetPinInput -> recoveryError = "New PINs do not match."
                                else -> {
                                    val resetOk = onResetPinWithRecovery(answerInput, resetPinInput)
                                    if (!resetOk) {
                                        recoveryError = "Recovery answer doesn't match. Try again."
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("recovery_submit_button")
                    ) {
                        Text("Verify & Reset PIN")
                    }
                }
            } else {
                Text(
                    text = "No security recovery answer was saved when this PIN was set. Unlock with your 4-digit PIN, then add a Recovery Answer in Settings > App Lock & PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.textSecondary
                )
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Back to PIN Keypad")
                }
            }
        }
    }
}

@Composable
private fun PinKeypadButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    val ext = LocalSaathiColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(ext.surfaceElevated, ext.surfaceSubtle)
                )
            )
            .border(1.dp, ext.cardBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
