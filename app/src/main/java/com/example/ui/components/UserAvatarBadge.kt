package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalSaathiColors

enum class AvatarCategory(val label: String) {
    ALL("All"),
    CHARACTERS("Characters"),
    ICONS_3D("3D Icons")
}

@Immutable
data class AvatarOption(
    val key: String,
    val label: String,
    val category: AvatarCategory,
    @DrawableRes val drawableRes: Int? = null,
    val icon: ImageVector? = null,
    val gradientColors: List<Color> = listOf(Color(0xFF4F46E5), Color(0xFF0D9F6E)),
    val accentGlow: Color = Color(0xFFFBBF24)
)

val SaathiAvatarCatalog: List<AvatarOption> = listOf(
    AvatarOption(
        key = "initials",
        label = "Initials",
        category = AvatarCategory.CHARACTERS,
        gradientColors = listOf(Color(0xFF0D9F6E), Color(0xFF4F46E5)),
        accentGlow = Color(0xFFFBBF24)
    ),
    AvatarOption(
        key = "char_aarav",
        label = "Aarav",
        category = AvatarCategory.CHARACTERS,
        drawableRes = R.drawable.img_avatar_char_aarav,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF0D9F6E))
    ),
    AvatarOption(
        key = "char_priya",
        label = "Priya",
        category = AvatarCategory.CHARACTERS,
        drawableRes = R.drawable.img_avatar_char_priya,
        gradientColors = listOf(Color(0xFF0D9F6E), Color(0xFFD97706))
    ),
    AvatarOption(
        key = "3d_wealth",
        label = "Vault 3D",
        category = AvatarCategory.ICONS_3D,
        drawableRes = R.drawable.img_avatar_3d_wealth,
        gradientColors = listOf(Color(0xFF3730A3), Color(0xFF0D9F6E))
    ),
    AvatarOption(
        key = "3d_ai_orb",
        label = "Crystal 3D",
        category = AvatarCategory.ICONS_3D,
        drawableRes = R.drawable.img_ai_saathi_icon,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
    ),
    AvatarOption(
        key = "char_mascot",
        label = "Saathi Buddy",
        category = AvatarCategory.CHARACTERS,
        drawableRes = R.drawable.img_saathi_avatar,
        gradientColors = listOf(Color(0xFF087F5B), Color(0xFF10B981))
    ),
    AvatarOption(
        key = "3d_rocket",
        label = "Rocket 3D",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.RocketLaunch,
        gradientColors = listOf(Color(0xFF4338CA), Color(0xFF7C3AED)),
        accentGlow = Color(0xFFFBBF24)
    ),
    AvatarOption(
        key = "3d_crown",
        label = "Gold Crown",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.WorkspacePremium,
        gradientColors = listOf(Color(0xFFB45309), Color(0xFFF59E0B)),
        accentGlow = Color(0xFFFEF08A)
    ),
    AvatarOption(
        key = "3d_diamond",
        label = "Sapphire 3D",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.Diamond,
        gradientColors = listOf(Color(0xFF0369A1), Color(0xFF38BDF8)),
        accentGlow = Color(0xFFE0F2FE)
    ),
    AvatarOption(
        key = "3d_shield",
        label = "Shield 3D",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.VerifiedUser,
        gradientColors = listOf(Color(0xFF065F46), Color(0xFF10B981)),
        accentGlow = Color(0xFFA7F3D0)
    ),
    AvatarOption(
        key = "3d_piggy",
        label = "Smart Saver",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.Savings,
        gradientColors = listOf(Color(0xFFBE185D), Color(0xFFF472B6)),
        accentGlow = Color(0xFFFDE68A)
    ),
    AvatarOption(
        key = "3d_lotus",
        label = "Zen Mint",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.Spa,
        gradientColors = listOf(Color(0xFF0F766E), Color(0xFF2DD4BF)),
        accentGlow = Color(0xFFCCFBF1)
    ),
    AvatarOption(
        key = "3d_globe",
        label = "Jetsetter",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.FlightTakeoff,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFFEC4899)),
        accentGlow = Color(0xFFFBCFE8)
    ),
    AvatarOption(
        key = "3d_bolt",
        label = "Thunder 3D",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.Bolt,
        gradientColors = listOf(Color(0xFF0D9F6E), Color(0xFF0284C7)),
        accentGlow = Color(0xFFFEF08A)
    ),
    AvatarOption(
        key = "3d_trophy",
        label = "Champion",
        category = AvatarCategory.CHARACTERS,
        icon = Icons.Filled.EmojiEvents,
        gradientColors = listOf(Color(0xFFD97706), Color(0xFFEA580C)),
        accentGlow = Color(0xFFFEF9C3)
    ),
    AvatarOption(
        key = "3d_spark",
        label = "Nova Spark",
        category = AvatarCategory.ICONS_3D,
        icon = Icons.Filled.AutoAwesome,
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
        accentGlow = Color(0xFFF5D0FE)
    )
)

fun findAvatarOption(key: String): AvatarOption {
    return SaathiAvatarCatalog.find { it.key == key } ?: SaathiAvatarCatalog.first()
}

fun computeSaathiUserInitials(userName: String): String {
    val parts = userName.trim()
        .split(Regex("\\s+"))
        .map { token -> token.filter { it.isLetterOrDigit() } }
        .filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "SS"
    }
}

@Composable
fun SaathiUserAvatar(
    avatarKey: String,
    userName: String,
    size: Dp = 52.dp,
    showRing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    val option = remember(avatarKey) { findAvatarOption(avatarKey) }
    val initials = remember(userName) { computeSaathiUserInitials(userName) }

    val ringBrush = Brush.sweepGradient(
        colors = listOf(
            ext.accentPrimary,
            ext.heroGradientStart,
            ext.accentPrimary
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (showRing) {
                    Modifier.border(width = 2.dp, brush = ringBrush, shape = CircleShape)
                } else {
                    Modifier
                }
            )
    ) {
        when {
            option.drawableRes != null -> {
                Image(
                    painter = painterResource(id = option.drawableRes),
                    contentDescription = "${option.label} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            option.icon != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(option.gradientColors))
                ) {
                    // Glossy 3D studio sphere highlight
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    option.accentGlow.copy(alpha = 0.48f),
                                    Color.Transparent
                                ),
                                center = Offset(this.size.width * 0.30f, this.size.height * 0.25f),
                                radius = this.size.minDimension * 0.55f
                            ),
                            center = Offset(this.size.width * 0.30f, this.size.height * 0.25f),
                            radius = this.size.minDimension * 0.55f
                        )
                    }
                    Icon(
                        imageVector = option.icon,
                        contentDescription = "${option.label} avatar",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.50f)
                    )
                }
            }

            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                            )
                        )
                        .semantics {
                            contentDescription = "Initials avatar $initials"
                        }
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.34f).sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
