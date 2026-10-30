package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.components.SaathiBrandLogo
import com.example.ui.components.SaathiCard
import com.example.ui.theme.LocalSaathiColors
import com.example.util.ApkShareHelper

private const val DEV_NAME = "MD Azad"
private const val DEV_ROLE = "Full-Stack & Android App Developer"
private const val DEV_MOTTO = "Dream • Plan • Do"
private const val DEV_EMAIL = "Web.mdazad@gmail.com"
private const val DEV_WEBSITE_DISPLAY = "mdazads.vercel.app"
private const val DEV_WEBSITE_URL = "https://mdazads.vercel.app"
private const val DEV_GITHUB_DISPLAY = "github.com/mdazadas"
private const val DEV_GITHUB_URL = "https://github.com/mdazadas"

@Composable
fun AboutDeveloperScreen(
    onBack: () -> Unit,
    onStatusToast: (String) -> Unit,
    onOpenLegalTab: (LegalDocumentTab) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalSaathiColors.current
    var showFullPortraitDialog by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_about_developer_screen")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
        ) {
            // Pinned Top Bar so Back is always reachable at any scroll depth
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "About Developer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Creator profile, contact & app credits",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("settings_about_developer_list")
            ) {

            // 1. Prominent Uncropped Full Photo Showcase + Close-Up Face Identity Strip
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 26.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_full_photo_card")
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ext.heroGradientStart,
                                            ext.accentPrimary,
                                            ext.accentTertiary
                                        )
                                    )
                                )
                        )

                        // Full 1:1 Uncropped HD Portrait so both the full photo & MD Azad's face are crystal clear
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE7E2D8))
                                .border(1.dp, ext.cardBorder, RoundedCornerShape(20.dp))
                                .clickable { showFullPortraitDialog = true }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_developer_md_azad_card),
                                contentDescription = "Full portrait photo of developer MD Azad",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x990F172A),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ZoomIn,
                                        contentDescription = "Zoom full photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Full HD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Identity Strip with Close-Up Face Portrait + Name + Role + Motto
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable { showFullPortraitDialog = true }
                                        .testTag("developer_photo_avatar")
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_developer_md_azad),
                                        contentDescription = "Close-up face photo of MD Azad",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .align(Alignment.TopStart)
                                            .clip(CircleShape)
                                            .border(
                                                width = 2.dp,
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        ext.accentPrimary,
                                                        ext.accentTertiary,
                                                        ext.heroGradientStart,
                                                        ext.accentPrimary
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(ext.accentPrimary)
                                            .testTag("developer_verified_badge")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Verified,
                                            contentDescription = "Verified Developer",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = DEV_NAME,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = DEV_ROLE,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ext.accentPrimaryContainer,
                                border = BorderStroke(1.dp, ext.accentPrimary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = DEV_MOTTO,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.accentPrimary,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Developer Bio Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "About the Creator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Passionate about building fast, privacy-first Android and modern web applications that solve real everyday problems. Salary Saathi was crafted to give salaried professionals and freelancers effortless payday-to-payday clarity—combining local Room security, smart UPI receipt tracking, and bilingual AI guidance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. Direct Contact, Official Website & GitHub Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Contact & Portfolio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )

                        DeveloperContactItem(
                            icon = Icons.Outlined.Email,
                            label = "Email",
                            value = DEV_EMAIL,
                            accentColor = ext.accentPrimary,
                            accentContainer = ext.accentPrimaryContainer,
                            rowTestTag = "dev_email_row",
                            copyTestTag = "copy_dev_email_button",
                            onOpen = {
                                openEmailIntent(context, DEV_EMAIL, onStatusToast)
                            },
                            onCopy = {
                                copyDeveloperField(context, "Developer Email", DEV_EMAIL)
                                onStatusToast("Copied $DEV_EMAIL")
                            }
                        )

                        HorizontalDivider(
                            color = ext.cardBorder,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )

                        DeveloperContactItem(
                            icon = Icons.Outlined.Language,
                            label = "Website & Portfolio",
                            value = DEV_WEBSITE_DISPLAY,
                            accentColor = ext.accentSecondary,
                            accentContainer = ext.accentSecondaryContainer,
                            rowTestTag = "dev_website_row",
                            copyTestTag = "copy_dev_website_button",
                            onOpen = {
                                openExternalUrl(context, DEV_WEBSITE_URL, onStatusToast)
                            },
                            onCopy = {
                                copyDeveloperField(context, "Developer Website", DEV_WEBSITE_URL)
                                onStatusToast("Copied $DEV_WEBSITE_DISPLAY")
                            }
                        )

                        HorizontalDivider(
                            color = ext.cardBorder,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )

                        DeveloperContactItem(
                            icon = Icons.Outlined.Code,
                            label = "GitHub",
                            value = DEV_GITHUB_DISPLAY,
                            accentColor = ext.accentTertiary,
                            accentContainer = ext.accentTertiaryContainer,
                            rowTestTag = "dev_github_row",
                            copyTestTag = "copy_dev_github_button",
                            onOpen = {
                                openExternalUrl(context, DEV_GITHUB_URL, onStatusToast)
                            },
                            onCopy = {
                                copyDeveloperField(context, "Developer GitHub", DEV_GITHUB_URL)
                                onStatusToast("Copied $DEV_GITHUB_DISPLAY")
                            }
                        )
                    }
                }
            }

            // 4. Social & Quick Action Buttons
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Quick Connect",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = { openExternalUrl(context, DEV_WEBSITE_URL, onStatusToast) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("dev_open_portfolio_button")
                            ) {
                                Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Portfolio", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { openExternalUrl(context, DEV_GITHUB_URL, onStatusToast) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("dev_open_github_button")
                            ) {
                                Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { openEmailIntent(context, DEV_EMAIL, onStatusToast) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("dev_send_email_button")
                            ) {
                                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email Me", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    ApkShareHelper.shareInstalledApk(context, scope, onStatusToast)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("dev_share_app_button")
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share APK", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 5. App Info & Architecture Credits Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaathiBrandLogo(
                                size = 48.dp,
                                cornerRadius = 14.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Salary Saathi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version 1.0 • Smart Payday-to-Payday Companion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AppHighlightRow(
                            icon = Icons.Outlined.Shield,
                            text = "100% On-Device Room database with .json Backup & 4-Digit PIN Lock"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppHighlightRow(
                            icon = Icons.Outlined.AutoAwesome,
                            text = "Smart Bank/UPI Digital Receipts & Fast Bilingual AI Saathi"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onOpenLegalTab(LegalDocumentTab.PRIVACY_POLICY) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("about_dev_privacy_button")
                            ) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Privacy Policy", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { onOpenLegalTab(LegalDocumentTab.TERMS_OF_USE) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("about_dev_terms_button")
                            ) {
                                Icon(Icons.Outlined.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Terms & Safety", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = ext.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Designed & Developed by MD Azad",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            }
        }
    }

    // Pinch-to-Zoom Full-Screen HD Photo Viewer
    if (showFullPortraitDialog) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
            val nextScale = (scale * zoomChange).coerceIn(1f, 3.2f)
            scale = nextScale
            offset = if (nextScale > 1f) offset + offsetChange else Offset.Zero
        }

        Dialog(
            onDismissRequest = { showFullPortraitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEB040B14))
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Column {
                                Text(
                                    text = "$DEV_NAME • $DEV_MOTTO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pinch with two fingers to zoom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { showFullPortraitDialog = false },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close photo preview")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFE7E2D8))
                                .transformable(state = transformState)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_developer_md_azad_card),
                                contentDescription = "MD Azad full portrait card",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperContactItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    accentContainer: Color,
    rowTestTag: String,
    copyTestTag: String,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clickable(onClick = onOpen)
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .testTag(rowTestTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentContainer)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(copyTestTag)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = ext.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open $label",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@Composable
private fun AppHighlightRow(
    icon: ImageVector,
    text: String
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ext.accentPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ext.textSecondary
        )
    }
}

private fun copyDeveloperField(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun openExternalUrl(
    context: Context,
    url: String,
    onStatusToast: (String) -> Unit
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        onStatusToast("Could not open browser for $url")
    }
}

private fun openEmailIntent(
    context: Context,
    email: String,
    onStatusToast: (String) -> Unit
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email?subject=${Uri.encode("Salary Saathi App Feedback")}")
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        copyDeveloperField(context, "Developer Email", email)
        onStatusToast("No mail app found — copied $email")
    }
}

private fun shareAppInfo(context: Context) {
    val shareText = buildString {
        appendLine("Salary Saathi — Smart Payday-to-Payday Budget & UPI Companion")
        appendLine("Developed by $DEV_NAME ($DEV_MOTTO)")
        appendLine("Portfolio: $DEV_WEBSITE_URL")
        append("GitHub: $DEV_GITHUB_URL")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Salary Saathi App")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        val chooser = Intent.createChooser(intent, "Share Salary Saathi").apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }
}
