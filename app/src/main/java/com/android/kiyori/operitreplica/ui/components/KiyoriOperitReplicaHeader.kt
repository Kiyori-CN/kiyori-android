package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R

@Composable
internal fun KiyoriOperitReplicaHeader(
    showHistoryPanel: Boolean,
    immersiveMode: Boolean,
    currentCharacterLabel: String,
    usageProgress: Float,
    contextUsagePercentage: Float,
    currentWindowSize: Int,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showStatsMenu: Boolean,
    onHistoryClick: () -> Unit,
    onPipClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onStatsMenuDismiss: () -> Unit,
    onStatsMenuToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperitSecondaryIconButton(
            icon = Icons.Default.History,
            contentDescription = "history",
            selected = showHistoryPanel,
            selectedTint = Color(0xFF4568B2),
            onClick = onHistoryClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        OperitSecondaryIconButton(
            icon = Icons.Default.PictureInPicture,
            contentDescription = "pip",
            selected = immersiveMode,
            selectedTint = Color(0xFF4568B2),
            onClick = onPipClick,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(onClick = onCharacterClick),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_kiyori_operit_avatar),
                    contentDescription = "Operit avatar",
                    modifier = Modifier.size(24.dp).clip(CircleShape),
                )
                Text(
                    text = currentCharacterLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 116.dp),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF667085),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onStatsMenuToggle() },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = usageProgress,
                modifier = Modifier.fillMaxSize(),
                color =
                    when {
                        contextUsagePercentage > 90f -> Color(0xFFDC2626)
                        contextUsagePercentage > 75f -> Color(0xFFF59E0B)
                        else -> Color(0xFF6177B2)
                    },
                trackColor = Color(0xFFF1F2F6),
                strokeWidth = 2.8.dp,
            )
            Text(
                text = "${contextUsagePercentage.toInt()}",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5A5E92),
            )
            DropdownMenu(
                expanded = showStatsMenu,
                onDismissRequest = onStatsMenuDismiss,
                modifier = Modifier.background(Color.White),
            ) {
                DropdownMenuItem(text = { Text("Context Window: $currentWindowSize") }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text("Input Tokens: $inputTokenCount") }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text("Output Tokens: $outputTokenCount") }, onClick = {}, enabled = false)
                DropdownMenuItem(
                    text = { Text("Total Tokens: ${inputTokenCount + outputTokenCount}") },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

@Composable
internal fun OperitSecondaryIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    selectedTint: Color = Color(0xFF4568B2),
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) selectedTint.copy(alpha = 0.14f) else Color.Transparent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) selectedTint else Color(0xFF3F3F46),
            modifier = Modifier.size(20.dp),
        )
    }
}
