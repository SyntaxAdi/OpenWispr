package com.openwispr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R

enum class NavTab {
    Home, History, Stats, Settings
}

@Composable
fun BottomNavDock(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Dock Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(Color(0xFF0D0E12), shape = RoundedCornerShape(34.dp))
                .border(1.dp, Color(0xFF20222A), shape = RoundedCornerShape(34.dp))
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 items
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockItem(
                        iconRes = R.drawable.ic_home,
                        label = "Home",
                        isSelected = currentTab == NavTab.Home,
                        onClick = { onTabSelected(NavTab.Home) }
                    )
                    DockItem(
                        iconRes = R.drawable.ic_history,
                        label = "History",
                        isSelected = currentTab == NavTab.History,
                        onClick = { onTabSelected(NavTab.History) }
                    )
                }

                // Space for center Mic HUD
                Spacer(modifier = Modifier.width(64.dp))

                // Right 2 items
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockItem(
                        iconRes = R.drawable.ic_stats,
                        label = "Stats",
                        isSelected = currentTab == NavTab.Stats,
                        onClick = { onTabSelected(NavTab.Stats) }
                    )
                    DockItem(
                        iconRes = R.drawable.ic_settings,
                        label = "Settings",
                        isSelected = currentTab == NavTab.Settings,
                        onClick = { onTabSelected(NavTab.Settings) }
                    )
                }
            }
        }

        // Central Concentric-Ring Mic HUD Button
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer dotted ring / dial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0x33FF2E2E), CircleShape)
                    .padding(4.dp)
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            )

            // Inner Red Mic Action Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14151C))
                    .border(1.5.dp, Color(0xFFFF2E2E), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMicClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "Microphone",
                    tint = Color(0xFFFF2E2E),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFFF2E2E)
    val inactiveColor = Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else inactiveColor
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(activeColor, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
