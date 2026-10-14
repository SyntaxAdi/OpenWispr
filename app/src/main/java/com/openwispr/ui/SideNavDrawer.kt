package com.openwispr.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.NavTab
import com.openwispr.R

private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorCardElevated = Color(0xFF101014)
private val ColorCardSelected = Color(0xFF16161E)
private val ColorBorder = Color(0xFF141416)
private val ColorBorderSelected = Color(0x66FF2E2E)
private val ColorMuted = Color(0xFF8E8E93)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorGreenAccent = Color(0xFF00E676)

/**
 * Side navigation drawer.
 * Built with Pitch Black OLED surfaces, dot-matrix branding, tactile squircle
 * navigation cards, and a real-time hardware telemetry status widget.
 */
@Composable
fun SideNavDrawer(
    currentTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    onClose: () -> Unit,
    isAccessibilityActive: Boolean,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Breathing cycle for glowing title
    val infiniteTransition = rememberInfiniteTransition(label = "SideNavBreathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SideNavGlow"
    )
    val glowingRed = ColorRedAccent.copy(alpha = breathingAlpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Header: Dot-Matrix Title + Close Button Squircle on exact same level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DotMatrixText(
                            text = "Open ",
                            dotSize = 2.6.dp,
                            dotSpacing = 0.8.dp,
                            charSpacing = 1.8.dp,
                            color = Color.White
                        )
                        DotMatrixText(
                            text = "Wispr",
                            dotSize = 2.6.dp,
                            dotSpacing = 0.8.dp,
                            charSpacing = 1.8.dp,
                            color = glowingRed
                        )
                    }

                    // Tactile Close Button level-aligned with title
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorCardElevated)
                            .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close Menu",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Divider hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorBorder)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Navigation Section Monospace Label
                Text(
                    text = "// NAVIGATION",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Navigation Cards
                SideNavCardItem(
                    title = "HOME",
                    subtitle = "Telemetry & statistics",
                    iconRes = R.drawable.ic_home,
                    isSelected = currentTab == NavTab.Home || currentTab == NavTab.Stats,
                    onClick = { onSelectTab(NavTab.Home) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SideNavCardItem(
                    title = "HISTORY",
                    subtitle = "Dictation logs & archive",
                    iconRes = R.drawable.ic_history,
                    isSelected = currentTab == NavTab.History,
                    onClick = { onSelectTab(NavTab.History) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SideNavCardItem(
                    title = "SETTINGS",
                    subtitle = "Engine & permissions",
                    iconRes = R.drawable.ic_settings,
                    isSelected = currentTab == NavTab.Settings,
                    onClick = { onSelectTab(NavTab.Settings) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. System Telemetry Section Monospace Label
                Text(
                    text = "// SYSTEM TELEMETRY",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Hardware System Telemetry Card
                DrawerTelemetryCard(
                    isAccessibilityActive = isAccessibilityActive,
                    hasMicPermission = hasMicPermission
                )
            }

            // 6. Author Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorBorder)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/syntaxadi")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Made With ❤️ By ",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorMuted
                    )
                    Text(
                        text = "Λ∂ιтуα",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ColorRedAccent
                    )
                }
            }
        }
    }
}

/**
 * Tactile navigation card item.
 */
@Composable
private fun SideNavCardItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isSelected) ColorCardSelected else ColorCardBg
    val borderCol = if (isSelected) ColorBorderSelected else ColorBorder
    val iconTileBg = if (isSelected) Color(0xFF1F1F2C) else ColorCardElevated
    val iconTint = if (isSelected) ColorRedAccent else ColorMuted
    val titleColor = if (isSelected) Color.White else Color(0xFFE0E0E0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Squircle Icon Tile
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTileBg)
                        .border(1.dp, ColorBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = titleColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = ColorMuted
                    )
                }
            }

            // Right Status Indicator
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ColorRedAccent, CircleShape)
                    )
                    Text(
                        text = "ACTIVE",
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorRedAccent,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Text(
                    text = "• • •",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0x33FFFFFF)
                )
            }
        }
    }
}

/**
 * Hardware-inspired telemetry status panel.
 */
@Composable
private fun DrawerTelemetryCard(
    isAccessibilityActive: Boolean,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE STATUS",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "HARDWARE",
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorMuted
                )
            }

            // Status Row 1: Accessibility Service
            TelemetryStatusRow(
                label = "ACCESSIBILITY",
                value = if (isAccessibilityActive) "ONLINE" else "STANDBY",
                dotColor = if (isAccessibilityActive) ColorGreenAccent else ColorRedAccent
            )

            // Status Row 2: Microphone
            TelemetryStatusRow(
                label = "MICROPHONE",
                value = if (hasMicPermission) "READY" else "LOCKED",
                dotColor = if (hasMicPermission) ColorGreenAccent else Color(0xFFFFB300)
            )

            // Status Row 3: Audio Spec
            TelemetryStatusRow(
                label = "AUDIO PIPE",
                value = "16 kHz MONO",
                dotColor = ColorMuted
            )

            // Dot-matrix decor bar
            DotMatrixDualProgressBar(
                progress = if (isAccessibilityActive && hasMicPermission) 1.0f else 0.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TelemetryStatusRow(
    label: String,
    value: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = ColorMuted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}
