package com.android.kiyori.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.manager.PreferencesManager
import com.android.kiyori.ui.compose.ImmersiveTopAppBar
import com.android.kiyori.ui.compose.SettingsColors as SettingsPalette

/**
 * Compose 版本的播放设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            ImmersiveTopAppBar(
                title = { Text("播放设置", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        PlaybackSettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PlaybackSettingsContent(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = SettingsPalette.SectionHeaderText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        letterSpacing = 0.05.sp
    )
}

@Composable
fun SwitchSettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsPalette.PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = SettingsPalette.SecondaryText
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SettingsPalette.Divider
                )
            )
        }
    }
}

@Composable
fun ClickableSettingCard(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsPalette.PrimaryText,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = SettingsPalette.AccentText,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SeekTimeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(3, 5, 10, 15, 20, 25, 30)
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "快进/快退时长",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.PrimaryText
            )
        },
        text = {
            Column(modifier = Modifier.width(280.dp)) {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .background(
                                if (selected == seconds) SettingsPalette.Highlight
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${seconds}秒",
                            fontSize = 15.sp,
                            color = if (selected == seconds) accentColor else SettingsPalette.PrimaryText,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(text = "确定", color = SettingsPalette.AccentText, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = SettingsPalette.SecondaryText, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface,
        modifier = Modifier.width(320.dp)
    )
}

@Composable
fun DoubleTapModeCard(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "双击手势",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsPalette.PrimaryText
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (currentMode == 0) SettingsPalette.Highlight
                        else Color.Transparent
                    )
                    .clickable { onModeChange(0) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == 0,
                    onClick = { onModeChange(0) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "暂停/播放",
                        fontSize = 15.sp,
                        color = if (currentMode == 0) MaterialTheme.colorScheme.primary else SettingsPalette.PrimaryText,
                        fontWeight = if (currentMode == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "双击任意位置暂停或播放",
                        fontSize = 12.sp,
                        color = SettingsPalette.SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (currentMode == 1) SettingsPalette.Highlight
                        else Color.Transparent
                    )
                    .clickable { onModeChange(1) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == 1,
                    onClick = { onModeChange(1) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "快进/快退",
                        fontSize = 15.sp,
                        color = if (currentMode == 1) MaterialTheme.colorScheme.primary else SettingsPalette.PrimaryText,
                        fontWeight = if (currentMode == 1) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "双击左半屏快退，右半屏快进",
                        fontSize = 12.sp,
                        color = SettingsPalette.SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(5, 10, 15, 20, 30)
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "双击跳转时长",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.PrimaryText
            )
        },
        text = {
            Column(modifier = Modifier.width(280.dp)) {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .background(
                                if (selected == seconds) SettingsPalette.Highlight
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${seconds}秒",
                            fontSize = 15.sp,
                            color = if (selected == seconds) accentColor else SettingsPalette.PrimaryText,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(text = "确定", color = SettingsPalette.AccentText, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = SettingsPalette.SecondaryText, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface,
        modifier = Modifier.width(320.dp)
    )
}
