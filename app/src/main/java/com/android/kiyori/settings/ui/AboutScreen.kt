package com.android.kiyori.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R
import com.android.kiyori.app.AppConstants
import com.android.kiyori.ui.compose.ImmersiveTopAppBar
import com.android.kiyori.ui.compose.SettingsColors as SettingsPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text("关于", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App 信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsPalette.CardBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App 图标
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Kiyori",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsPalette.PrimaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Version $versionName",
                        fontSize = 14.sp,
                        color = SettingsPalette.SecondaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "浏览器优先的网页与媒体工具",
                        fontSize = 14.sp,
                        color = SettingsPalette.SecondaryText
                    )
                }
            }

            // 功能列表
            AboutItem(
                icon = Icons.Default.Code,
                title = "开源主页",
                subtitle = "访问 GitHub 仓库",
                onClick = {
                    openUrl(context, AppConstants.URLs.GITHUB_URL)
                }
            )

            AboutItem(
                icon = Icons.Default.Description,
                title = "许可证书",
                subtitle = "查看开源许可",
                onClick = onNavigateToLicense
            )

            AboutItem(
                icon = Icons.Default.BugReport,
                title = "意见反馈",
                subtitle = "报告问题或建议",
                onClick = onNavigateToFeedback
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsPalette.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SettingsPalette.IconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsPalette.PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = SettingsPalette.SecondaryText
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SettingsPalette.TertiaryText
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

