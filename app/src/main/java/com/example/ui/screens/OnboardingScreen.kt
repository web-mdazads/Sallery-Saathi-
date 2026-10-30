package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiCategoryDraft
import com.example.data.BudgetHealth
import com.example.data.IncentiveMode
import com.example.ui.components.SaathiCard
import com.example.ui.components.SmoothBudgetProgressBar
import com.example.ui.components.SurplusPriorityChoices
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import com.example.viewmodel.AiPlannerUiState
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    currencySymbol: String,
    aiOptInEnabled: Boolean,
    plannerState: AiPlannerUiState,
    onToggleAiOptIn: (Boolean) -> Unit,
    onSkipToEmptyDashboard: () -> Unit,
    onSaveProfileStep: (
        userName: String,
        fixedSalary: Double,
        expectedIncentive: Double,
        incentiveMode: IncentiveMode,
        paydayDay: Int,
        markOnboardingDone: Boolean
    ) -> Unit,
    onSaveSelectedGoals: (Set<String>) -> Unit,
    onGenerateProposal: (
        income: Double,
        payday: Int,
        rent: Double,
        family: Double,
        emi: Double,
        bills: Double,
        surplusPriority: String,
        useGeminiAi: Boolean
    ) -> Unit,
    onUpdateDraft: (categoryId: String, newAmount: Double?, isIncluded: Boolean?) -> Unit,
    onAcceptAllSuggestions: () -> Unit,
    onConfirmAndFinishOnboarding: (List<AiCategoryDraft>) -> Unit
) {
    val ext = LocalSaathiColors.current
    val currencyLabel = FinanceUtils.currencyLabel(currencySymbol)
    var step by remember { mutableIntStateOf(1) }
    var activeLegalModalTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    var userName by remember { mutableStateOf("") }
    var salaryInput by remember { mutableStateOf("") }
    var expectedIncentiveInput by remember { mutableStateOf("") }
    var incentiveMode by remember { mutableStateOf(IncentiveMode.ONLY_RECEIVED) }
    var paydayDay by remember { mutableIntStateOf(1) }

    var rentInput by remember { mutableStateOf("") }
    var familyInput by remember { mutableStateOf("") }
    var emiInput by remember { mutableStateOf("") }
    var billsInput by remember { mutableStateOf("") }

    var selectedGoals by remember { mutableStateOf(setOf("emergency", "future_savings")) }
    var surplusPriority by remember { mutableStateOf(SurplusPriorityChoices.first()) }

    BackHandler(enabled = step > 1) {
        step -= 1
    }

    val parsedSalary = salaryInput.replace(",", "").toDoubleOrNull() ?: 0.0
    val parsedIncentive = expectedIncentiveInput.replace(",", "").toDoubleOrNull() ?: 0.0

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Progress & Navigation Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (step > 1) {
                        IconButton(
                            onClick = { step -= 1 },
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ext.surfaceSubtle)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Step $step of 8",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            if (parsedSalary > 0.0) {
                                onSaveProfileStep(
                                    userName,
                                    parsedSalary,
                                    parsedIncentive,
                                    incentiveMode,
                                    paydayDay,
                                    true
                                )
                            } else {
                                onSkipToEmptyDashboard()
                            }
                        },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segmented 8-Pill Luxury Progress Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (idx in 1..8) {
                        val segColor by animateColorAsState(
                            targetValue = when {
                                idx <= step -> MaterialTheme.colorScheme.primary
                                else -> ext.surfaceSubtle
                            },
                            label = "onboarding_pill_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(segColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        if (targetState > initialState) {
                            (fadeIn() + slideInHorizontally(slideSpec) { it / 6 }) togetherWith
                                (fadeOut() + slideOutHorizontally(slideSpec) { -it / 6 })
                        } else {
                            (fadeIn() + slideInHorizontally(slideSpec) { -it / 6 }) togetherWith
                                (fadeOut() + slideOutHorizontally(slideSpec) { it / 6 })
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "onboarding_step_content"
                ) { currentStep ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (currentStep) {
                            1 -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(216.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_onboarding_hero),
                                        contentDescription = "Salary Saathi illustration",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0x88042F22))
                                                )
                                            )
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(14.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xD9064E38))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFF6EE7B7),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Private & Local",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "Welcome to Salary Saathi AI",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Smart salary budgeting, UPI alerts, and goal tracking.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ext.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { step = 2 },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("onboarding_get_started_button")
                                ) {
                                    Text("Get Started", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = onSkipToEmptyDashboard,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Skip for now", maxLines = 1, softWrap = false)
                                }

                                // Onboarding Trust & Legal Footer
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "100% On-Device Storage • Review Legal & Permission Safety",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = { activeLegalModalTab = LegalDocumentTab.PRIVACY_POLICY },
                                            modifier = Modifier.testTag("onboarding_privacy_link")
                                        ) {
                                            Text("Privacy Policy", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text("•", color = ext.textSecondary, style = MaterialTheme.typography.labelSmall)
                                        TextButton(
                                            onClick = { activeLegalModalTab = LegalDocumentTab.TERMS_OF_USE },
                                            modifier = Modifier.testTag("onboarding_terms_link")
                                        ) {
                                            Text("Terms", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text("•", color = ext.textSecondary, style = MaterialTheme.typography.labelSmall)
                                        TextButton(
                                            onClick = { activeLegalModalTab = LegalDocumentTab.PERMISSIONS_SAFETY },
                                            modifier = Modifier.testTag("onboarding_permissions_link")
                                        ) {
                                            Text("Permissions", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            2 -> {
                                Text(
                                    text = "Monthly Salary",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Take-home pay after deductions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )
                                OutlinedTextField(
                                    value = userName,
                                    onValueChange = { userName = it },
                                    label = { Text("Name (optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = salaryInput,
                                    onValueChange = { salaryInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Monthly Salary ($currencyLabel)") },
                                    placeholder = { Text("45000") },
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_salary_input")
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(25000, 40000, 60000, 85000, 120000).forEach { preset ->
                                        FilterChip(
                                            selected = parsedSalary == preset.toDouble(),
                                            onClick = { salaryInput = preset.toString() },
                                            shape = RoundedCornerShape(50),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            label = {
                                                Text(
                                                    text = FinanceUtils.formatRupee(preset.toDouble(), false, currencySymbol),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { step = 3 },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("onboarding_next_2")
                                ) {
                                    Text("Continue", maxLines = 1, softWrap = false)
                                }
                            }

                            3 -> {
                                Text(
                                    text = "Incentives",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose how bonuses count toward your budget.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )
                                IncentiveMode.entries.forEach { mode ->
                                    val selected = incentiveMode == mode
                                    SaathiCard(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else ext.surfaceElevated,
                                        borderColor = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                        onClick = { incentiveMode = mode },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = mode.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ext.textSecondary
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = expectedIncentiveInput,
                                    onValueChange = { expectedIncentiveInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Expected Incentive ($currencyLabel, optional)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { step = 4 },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("Continue", maxLines = 1, softWrap = false)
                                }
                            }

                            4 -> {
                                Text(
                                    text = "Payday",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Day your salary arrives each month.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(1, 5, 7, 10, 15, 25, 28, 30, 31).forEach { day ->
                                        FilterChip(
                                            selected = paydayDay == day,
                                            onClick = { paydayDay = day },
                                            shape = RoundedCornerShape(50),
                                            label = { Text("Day $day") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                                val previewCycle = FinanceUtils.computeSalaryCycle(paydayDay)
                                SaathiCard(containerColor = ext.surfaceElevated) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = previewCycle.cycleLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${previewCycle.daysUntilPayday}d left",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ext.textSecondary
                                        )
                                    }
                                }
                                Button(
                                    onClick = { step = 5 },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("Continue")
                                }
                            }

                            5 -> {
                                Text(
                                    text = "Fixed Bills",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Optional monthly commitments.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )
                                OutlinedTextField(
                                    value = rentInput,
                                    onValueChange = { rentInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Rent ($currencyLabel)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = familyInput,
                                    onValueChange = { familyInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Family Support ($currencyLabel)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = emiInput,
                                    onValueChange = { emiInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Loan EMI ($currencyLabel)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = billsInput,
                                    onValueChange = { billsInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Utilities & Wi-Fi ($currencyLabel)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { step = 6 },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("Continue", maxLines = 1, softWrap = false)
                                }
                            }

                            6 -> {
                                Text(
                                    text = "Savings Goals",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pick your top priorities.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )
                                val goalOptions = listOf(
                                    "emergency" to "Emergency Fund",
                                    "future_savings" to "Future Savings",
                                    "family" to "Family Support Goals",
                                    "gadget" to "New Phone or Gadget",
                                    "vehicle" to "Bike or Car",
                                    "travel" to "Travel"
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    goalOptions.forEach { (key, label) ->
                                        val isPicked = key in selectedGoals
                                        FilterChip(
                                            selected = isPicked,
                                            onClick = {
                                                selectedGoals = if (isPicked) selectedGoals - key else selectedGoals + key
                                            },
                                            shape = RoundedCornerShape(50),
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Top surplus priority",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SurplusPriorityChoices.forEach { choice ->
                                        FilterChip(
                                            selected = surplusPriority == choice,
                                            onClick = { surplusPriority = choice },
                                            shape = RoundedCornerShape(50),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            label = { Text(choice) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSaveProfileStep(
                                            userName,
                                            parsedSalary,
                                            parsedIncentive,
                                            incentiveMode,
                                            paydayDay,
                                            false
                                        )
                                        onSaveSelectedGoals(selectedGoals)
                                        step = 7
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("Continue", maxLines = 1, softWrap = false)
                                }
                            }

                            7 -> {
                                Text(
                                    text = "Generate Budget",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${FinanceUtils.formatRupee(parsedSalary, false, currencySymbol)} salary • Payday $paydayDay • $surplusPriority",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textSecondary
                                )

                                SaathiCard(containerColor = ext.surfaceElevated) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Text(
                                            text = "AI Personalization",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = aiOptInEnabled,
                                            onCheckedChange = onToggleAiOptIn
                                        )
                                    }
                                }

                                if (plannerState is AiPlannerUiState.Generating) {
                                    SaathiCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(18.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Building plan...")
                                        }
                                    }
                                } else if (plannerState is AiPlannerUiState.Error) {
                                    SaathiCard(containerColor = ext.warningContainer) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = plannerState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ext.warning
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (!aiOptInEnabled) onToggleAiOptIn(true)
                                        onGenerateProposal(
                                            parsedSalary,
                                            paydayDay,
                                            rentInput.toDoubleOrNull() ?: 0.0,
                                            familyInput.toDoubleOrNull() ?: 0.0,
                                            emiInput.toDoubleOrNull() ?: 0.0,
                                            billsInput.toDoubleOrNull() ?: 0.0,
                                            surplusPriority,
                                            true
                                        )
                                        step = 8
                                    },
                                    enabled = plannerState !is AiPlannerUiState.Generating,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Suggestion", maxLines = 1, softWrap = false)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onGenerateProposal(
                                            parsedSalary,
                                            paydayDay,
                                            rentInput.toDoubleOrNull() ?: 0.0,
                                            familyInput.toDoubleOrNull() ?: 0.0,
                                            emiInput.toDoubleOrNull() ?: 0.0,
                                            billsInput.toDoubleOrNull() ?: 0.0,
                                            surplusPriority,
                                            false
                                        )
                                        step = 8
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Smart Formula", maxLines = 1, softWrap = false)
                                }
                            }

                            8 -> {
                                Text(
                                    text = "Review Budget",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                when (plannerState) {
                                    is AiPlannerUiState.Generating -> {
                                        SaathiCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(20.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Building plan...")
                                            }
                                        }
                                    }

                                    is AiPlannerUiState.Error -> {
                                        SaathiCard(containerColor = ext.warningContainer) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = plannerState.message,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = ext.warning
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        onGenerateProposal(
                                                            parsedSalary,
                                                            paydayDay,
                                                            rentInput.toDoubleOrNull() ?: 0.0,
                                                            familyInput.toDoubleOrNull() ?: 0.0,
                                                            emiInput.toDoubleOrNull() ?: 0.0,
                                                            billsInput.toDoubleOrNull() ?: 0.0,
                                                            surplusPriority,
                                                            false
                                                        )
                                                    }
                                                ) {
                                                    Text("Load Smart Formula Plan")
                                                }
                                            }
                                        }
                                    }

                                    is AiPlannerUiState.Ready -> {
                                        val proposal = plannerState.proposal
                                        val plannedSum = proposal.drafts.filter { it.isIncluded }.sumOf { it.editedAmount }
                                        val diff = parsedSalary - plannedSum
                                        val overBudget = parsedSalary > 0 && diff < -0.5

                                        Text(
                                            text = proposal.summaryNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.textSecondary
                                        )

                                        SaathiCard(
                                            containerColor = if (overBudget) ext.dangerContainer else ext.healthyContainer,
                                            borderColor = if (overBudget) ext.danger.copy(alpha = 0.35f) else ext.healthy.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (overBudget) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (overBudget) ext.danger else ext.healthy
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = when {
                                                            overBudget -> "Over salary by ${FinanceUtils.formatRupee(abs(diff), false, currencySymbol)}"
                                                            parsedSalary > 0 && diff > 0.5 -> "Planned ${FinanceUtils.formatRupee(plannedSum, false, currencySymbol)} • ${FinanceUtils.formatRupee(diff, false, currencySymbol)} left"
                                                            else -> "Planned ${FinanceUtils.formatRupee(plannedSum, false, currencySymbol)}"
                                                        },
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (overBudget) ext.danger else ext.healthy
                                                    )
                                                }
                                                if (parsedSalary > 0) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    SmoothBudgetProgressBar(
                                                        progressFraction = (plannedSum / parsedSalary).toFloat(),
                                                        health = if (overBudget) BudgetHealth.OVERSPENT else BudgetHealth.HEALTHY,
                                                        height = 7.dp
                                                    )
                                                }
                                            }
                                        }

                                        TextButton(onClick = onAcceptAllSuggestions) {
                                            Text("Reset amounts")
                                        }

                                        proposal.drafts.forEach { draft ->
                                            SaathiCard(
                                                containerColor = ext.surfaceElevated,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = draft.categoryName,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            val pct = if (parsedSalary > 0 && draft.isIncluded) {
                                                                FinanceUtils.formatPercentage((draft.editedAmount / parsedSalary) * 100.0)
                                                            } else "0%"
                                                            Text(
                                                                text = "$pct of salary • ${draft.reason}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = ext.textSecondary
                                                            )
                                                        }
                                                        Switch(
                                                            checked = draft.isIncluded,
                                                            onCheckedChange = { onUpdateDraft(draft.categoryId, null, it) }
                                                        )
                                                    }
                                                    if (draft.isIncluded) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        var fieldStr by remember(draft.categoryId, draft.editedAmount) {
                                                            mutableStateOf(draft.editedAmount.toLong().toString())
                                                        }
                                                        OutlinedTextField(
                                                            value = fieldStr,
                                                            onValueChange = { s ->
                                                                val clean = s.filter { c -> c.isDigit() }
                                                                fieldStr = clean
                                                                onUpdateDraft(draft.categoryId, clean.toDoubleOrNull() ?: 0.0, null)
                                                            },
                                                            label = { Text("Amount ($currencyLabel)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { onConfirmAndFinishOnboarding(proposal.drafts) },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .testTag("finish_onboarding_save_budget_button")
                                        ) {
                                            Text("Save Budget", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                    }

                                    AiPlannerUiState.Idle -> {
                                        OutlinedButton(onClick = { step = 7 }) {
                                            Text("Back to generate plan")
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

    activeLegalModalTab?.let { targetTab ->
        LegalPrivacyModalDialog(
            initialTab = targetTab,
            onDismiss = { activeLegalModalTab = null }
        )
    }
}
