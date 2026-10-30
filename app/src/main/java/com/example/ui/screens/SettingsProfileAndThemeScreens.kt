package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.ui.components.AvatarCategory
import com.example.ui.components.SaathiAvatarCatalog
import com.example.ui.components.SaathiCard
import com.example.ui.components.SaathiUserAvatar
import com.example.ui.components.findAvatarOption
import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.LocalSaathiColors
import com.example.ui.theme.ThemeMode
import com.example.util.FinanceUtils

@Composable
internal fun SettingsSubScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("settings_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to Settings"
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = ext.textSecondary
            )
        }
    }
}

@Composable
internal fun ProfileSummaryScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onEditProfile: () -> Unit
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current
    val avatarMeta = remember(preferences.avatarKey) { findAvatarOption(preferences.avatarKey) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_profile_summary_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_profile_summary_list")
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Your Profile",
                    subtitle = "Your saved identity, salary plan & security details",
                    onBack = onBack
                )
            }

            // Hero Identity Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SaathiUserAvatar(
                            avatarKey = preferences.avatarKey,
                            userName = preferences.userName,
                            size = 86.dp,
                            showRing = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = preferences.userName.ifBlank { "Your Profile" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = preferences.userRole.ifBlank { "${avatarMeta.label} Avatar • Salaried Earner" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        if (preferences.userMotto.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "“${preferences.userMotto}”",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.heroGlowAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Entered Personal Details Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Entered Profile Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ProfileSummaryRow("Display Name", preferences.userName.ifBlank { "Not entered yet" })
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Role / Occupation", preferences.userRole.ifBlank { "Not entered yet" })
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Email / Phone", preferences.userHandle.ifBlank { "Not entered yet" })
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Monthly Motto", preferences.userMotto.ifBlank { "Not entered yet" })
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Selected Avatar", "${avatarMeta.label} (${avatarMeta.category.label})")
                    }
                }
            }

            // Financial & App Preferences Summary Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Salary Plan & Security",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ProfileSummaryRow(
                            "Fixed Monthly Salary",
                            if (preferences.fixedMonthlySalary > 0) {
                                FinanceUtils.formatRupee(
                                    preferences.fixedMonthlySalary,
                                    preferences.showDecimalsInLists,
                                    preferences.currencySymbol
                                )
                            } else {
                                "Not set"
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow(
                            "Expected Bonus / Incentive",
                            if (preferences.expectedIncentiveAmount > 0) {
                                FinanceUtils.formatRupee(
                                    preferences.expectedIncentiveAmount,
                                    preferences.showDecimalsInLists,
                                    preferences.currencySymbol
                                )
                            } else {
                                "None"
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Salary Payday", "Day ${preferences.paydayDayOfMonth} of month")
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow("Active Theme", preferences.colorPalette.title)
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow(
                            "Currency & Date",
                            "${FinanceUtils.currencyLabel(preferences.currencySymbol)} • ${preferences.dateFormatPattern}${if (preferences.showDecimalsInLists) " • .00" else ""}"
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        ProfileSummaryRow(
                            "4-Digit PIN Lock",
                            if (preferences.appPinCode.length == 4) "Enabled (Auto-Lock Active)" else "Off"
                        )
                    }
                }
            }

            // Action Button -> Opens Edit Profile & Avatar Screen
            item {
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("open_edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile & Avatar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryRow(label: String, value: String) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ext.textSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSaveProfile: (
        userName: String,
        userRole: String,
        userHandle: String,
        userMotto: String,
        avatarKey: String
    ) -> Unit,
    onSaved: () -> Unit = onBack
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current
    val focusManager = LocalFocusManager.current

    var nameInput by remember(preferences.userName) { mutableStateOf(preferences.userName) }
    var roleInput by remember(preferences.userRole) { mutableStateOf(preferences.userRole) }
    var handleInput by remember(preferences.userHandle) { mutableStateOf(preferences.userHandle) }
    var mottoInput by remember(preferences.userMotto) { mutableStateOf(preferences.userMotto) }
    var selectedAvatarKey by remember(preferences.avatarKey) { mutableStateOf(preferences.avatarKey) }
    var selectedCategoryFilter by remember { mutableStateOf(AvatarCategory.ALL) }
    var showAllAvatars by remember { mutableStateOf(false) }

    val filteredAvatars = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == AvatarCategory.ALL) {
            SaathiAvatarCatalog
        } else {
            SaathiAvatarCatalog.filter { it.category == selectedCategoryFilter }
        }
    }
    val visibleAvatars = remember(filteredAvatars, showAllAvatars) {
        if (showAllAvatars) filteredAvatars else filteredAvatars.take(4)
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_profile_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_profile_list")
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Profile & Avatar",
                    subtitle = "Personalize your identity & choose an avatar",
                    onBack = onBack
                )
            }

            // Live Profile Identity Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SaathiUserAvatar(
                            avatarKey = selectedAvatarKey,
                            userName = nameInput,
                            size = 74.dp,
                            showRing = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nameInput.ifBlank { "Your Name" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = roleInput.ifBlank { "Salaried Earner • India" },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (mottoInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "“$mottoInput”",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.heroGlowAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Selectable Avatar Gallery
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Choose Avatar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${SaathiAvatarCatalog.size} styles",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AvatarCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategoryFilter == cat,
                                    onClick = { selectedCategoryFilter = cat },
                                    shape = RoundedCornerShape(50),
                                    label = { Text(cat.label, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Strictly 4 avatars per row; shows 4 initially, expands to all on Show More
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            visibleAvatars.chunked(4).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { option ->
                                        val isSelected = selectedAvatarKey == option.key
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                    else ext.surfaceSubtle.copy(alpha = 0.55f)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable { selectedAvatarKey = option.key }
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                                .testTag("avatar_option_${option.key}")
                                        ) {
                                            Box(contentAlignment = Alignment.BottomEnd) {
                                                SaathiUserAvatar(
                                                    avatarKey = option.key,
                                                    userName = nameInput,
                                                    size = 46.dp,
                                                    showRing = isSelected
                                                )
                                                if (isSelected) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (filteredAvatars.size > 4) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showAllAvatars = !showAllAvatars },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("toggle_show_more_avatars_button")
                            ) {
                                Icon(
                                    imageVector = if (showAllAvatars) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showAllAvatars) {
                                        "Show Less"
                                    } else {
                                        "Show More (${filteredAvatars.size - 4} more)"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Profile Details Form
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Personal Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        OutlinedTextField(
                            value = roleInput,
                            onValueChange = { roleInput = it },
                            label = { Text("Role / Occupation") },
                            placeholder = { Text("e.g., Software Engineer") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_role_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Software Engineer", "Product & Design", "Business & Sales", "Creator", "Student").forEach { presetRole ->
                                AssistChip(
                                    onClick = { roleInput = presetRole },
                                    shape = RoundedCornerShape(50),
                                    label = {
                                        Text(
                                            text = presetRole,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = handleInput,
                            onValueChange = { handleInput = it },
                            label = { Text("Email or Phone (Local Only)") },
                            placeholder = { Text("name@email.com or +91...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_handle_input")
                        )

                        OutlinedTextField(
                            value = mottoInput,
                            onValueChange = { mottoInput = it },
                            label = { Text("Monthly Money Motto") },
                            placeholder = { Text("Save 30% before spending") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_motto_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Save 30% first", "Smart UPI spender", "Debt-free goal").forEach { quote ->
                                AssistChip(
                                    onClick = { mottoInput = quote },
                                    shape = RoundedCornerShape(50),
                                    label = {
                                        Text(
                                            text = quote,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSaveProfile(
                                    nameInput,
                                    roleInput,
                                    handleInput,
                                    mottoInput,
                                    selectedAvatarKey
                                )
                                focusManager.clearFocus()
                                onSaved()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_profile_name_button")
                        ) {
                            Text(
                                text = "Save Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateColorPalette: (ColorPalettePreset) -> Unit,
    onUpdateFormatPreferences: (currency: String, dateFormat: String, showDecimals: Boolean) -> Unit
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current
    val latestPrefs by rememberUpdatedState(preferences)
    val previewNowMillis = remember { System.currentTimeMillis() }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_theme_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_theme_list")
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Theme & Colors",
                    subtitle = "Main color + 2 accents, currency & date format",
                    onBack = onBack
                )
            }

            // Live 1-Main + 2-Accents & Smooth Red/Green Preview Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = FinanceUtils.formatDate(previewNowMillis, preferences.dateFormatPattern),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.90f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preferences.colorPalette.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = FinanceUtils.formatRupee(
                                52450.0,
                                preferences.showDecimalsInLists,
                                preferences.currencySymbol
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.expenseRedContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "-${FinanceUtils.formatRupee(450.0, preferences.showDecimalsInLists, preferences.currencySymbol)} Expense",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.expenseRed,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.incomeGreenContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+${FinanceUtils.formatRupee(5000.0, preferences.showDecimalsInLists, preferences.currencySymbol)} Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.incomeGreen,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Light / Dark / System Mode
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Appearance Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
                                Triple(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
                                Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.SettingsBrightness)
                            ).forEach { (mode, label, icon) ->
                                val selected = preferences.themeMode == mode
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else ext.surfaceSubtle
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onUpdateThemeMode(mode) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else ext.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3-Color Mix Palettes
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "1 Main + 2 Supporting Accents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Large circle = Main dominant color • 2 small circles = supporting accents",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                        ColorPalettePreset.entries.forEach { preset ->
                            val isSelected = preferences.colorPalette == preset
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        else ext.surfaceSubtle.copy(alpha = 0.65f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onUpdateColorPalette(preset) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("palette_option_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 1 Larger Main Swatch + 2 Smaller Supporting Accent Swatches
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        preset.previewSwatches.forEachIndexed { idx, swatch ->
                                            Box(
                                                modifier = Modifier
                                                    .size(if (idx == 0) 30.dp else 19.dp)
                                                    .clip(CircleShape)
                                                    .background(swatch)
                                                    .border(
                                                        width = if (idx == 0) 2.dp else 1.5.dp,
                                                        color = MaterialTheme.colorScheme.surface,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected palette",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Number, Currency & Date Formatting
            item {
                val formatChipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )

                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Currency & Date Format",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Live Format Preview Box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.surfaceSubtle)
                                .border(1.dp, ext.cardBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Preview",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary
                                )
                                Text(
                                    text = FinanceUtils.formatRupee(
                                        125000.0,
                                        preferences.showDecimalsInLists,
                                        preferences.currencySymbol
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.testTag("format_live_preview_amount")
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = FinanceUtils.formatDate(previewNowMillis, preferences.dateFormatPattern),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.testTag("format_live_preview_date")
                                )
                            }
                        }

                        Text(
                            text = "Currency Symbol",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.textSecondary
                        )
                        val currencyOptions = listOf(
                            "₹" to "currency_chip_inr_symbol",
                            "INR " to "currency_chip_inr_code",
                            "$" to "currency_chip_usd",
                            "€" to "currency_chip_eur",
                            "£" to "currency_chip_gbp"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currencyOptions.forEach { (sym, tag) ->
                                val isSelected = FinanceUtils.currencyLabel(preferences.currencySymbol) == FinanceUtils.currencyLabel(sym)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onUpdateFormatPreferences(
                                            sym,
                                            latestPrefs.dateFormatPattern,
                                            latestPrefs.showDecimalsInLists
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = formatChipColors,
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    label = {
                                        Text(
                                            text = sym.trim(),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag(tag)
                                )
                            }
                        }

                        Text(
                            text = "Date Style",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.textSecondary
                        )
                        val dateFormats = listOf("dd MMM yyyy", "dd/MM/yyyy", "MMM dd, yyyy", "yyyy-MM-dd")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dateFormats.forEachIndexed { idx, fmt ->
                                val isSelected = preferences.dateFormatPattern == fmt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onUpdateFormatPreferences(
                                            latestPrefs.currencySymbol,
                                            fmt,
                                            latestPrefs.showDecimalsInLists
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = formatChipColors,
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    label = {
                                        Text(
                                            text = fmt,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("date_format_chip_$idx")
                                )
                            }
                        }

                        HorizontalDivider(color = ext.cardBorder)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateFormatPreferences(
                                            latestPrefs.currencySymbol,
                                            latestPrefs.dateFormatPattern,
                                            !latestPrefs.showDecimalsInLists
                                        )
                                    }
                                    .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = "Show Decimals (.00)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Display .00 across all balances, cards & lists",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary
                                )
                            }
                            Switch(
                                checked = preferences.showDecimalsInLists,
                                onCheckedChange = {
                                    onUpdateFormatPreferences(
                                        latestPrefs.currencySymbol,
                                        latestPrefs.dateFormatPattern,
                                        it
                                    )
                                },
                                modifier = Modifier.testTag("show_decimals_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}
