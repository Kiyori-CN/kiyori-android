package com.android.kiyori.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserAgreementScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue <= 0) 1f else {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            }.coerceIn(0f, 1f)
        }
    }
    val canConfirm = hasScrolledToBottom && isChecked
    val confirmAlpha by animateFloatAsState(
        targetValue = if (canConfirm) 1f else 0.56f,
        animationSpec = tween(180),
        label = "agreement_confirm_alpha"
    )

    LaunchedEffect(scrollProgress) {
        if (scrollProgress >= 0.985f) {
            hasScrolledToBottom = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F8FA)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
                    .padding(top = 24.dp, bottom = 176.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AgreementHeader()
                AgreementSummaryRow()
                AgreementContent()
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFFF7F8FA))
            ) {
                LinearProgressIndicator(
                    progress = scrollProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = primary,
                    trackColor = borderColor
                )
                AgreementFooter(
                    hasScrolledToBottom = hasScrolledToBottom,
                    isChecked = isChecked,
                    canConfirm = canConfirm,
                    confirmAlpha = confirmAlpha,
                    onCheckedChange = { isChecked = it },
                    onAgree = onAgree,
                    onDecline = onDecline
                )
            }
        }
    }
}

@Composable
private fun AgreementHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Kiyori",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "使用协议与隐私说明",
            fontSize = 18.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "首次进入前，请确认应用能力、权限边界、数据处理方式与第三方服务风险。内容保持简洁，但会影响您后续使用 Kiyori 的方式。",
            fontSize = 13.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "最后更新：2026年5月1日",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun AgreementSummaryRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(
            modifier = Modifier.weight(1f),
            title = "本地优先",
            description = "数据默认保存在设备"
        )
        SummaryPill(
            modifier = Modifier.weight(1f),
            title = "按需授权",
            description = "权限由功能触发"
        )
        SummaryPill(
            modifier = Modifier.weight(1f),
            title = "合规使用",
            description = "尊重版权与平台规则"
        )
    }
}

@Composable
private fun SummaryPill(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AgreementContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AgreementSection(
            icon = Icons.Default.VerifiedUser,
            title = "应用定位",
            body = "Kiyori 是以浏览器为主入口的综合工具，包含网页访问、视频嗅探、远程播放、本地媒体浏览、下载、WebDAV、字幕弹幕和播放历史等能力。",
            accent = MaterialTheme.colorScheme.primary
        ) {
            BulletPoint("应用免费开源，遵守 GPL-3.0-or-later 协议。")
            BulletPoint("部分能力依赖第三方网站、网络环境、账号状态或远程服务，实际可用性可能变化。")
        }

        AgreementSection(
            icon = Icons.Default.PrivacyTip,
            title = "数据与隐私",
            body = "Kiyori 不提供自建账号体系，也不会把常规使用数据上传到 Kiyori 自有服务器。书签、历史、播放进度、下载配置和扩展数据默认存储在本机。",
            accent = Color(0xFF0E7C66)
        ) {
            CheckPoint("网页登录、WebDAV、远程链接输入等凭证只用于对应功能。")
            CheckPoint("敏感凭证优先保存在本地，并尽量使用系统安全能力保护。")
            CheckPoint("卸载应用后，应用私有目录中的本地数据通常会被系统清理。")
        }

        AgreementSection(
            icon = Icons.Default.Security,
            title = "权限调用",
            body = "应用会根据功能按需使用网络、媒体读取、文件访问、通知、摄像头、麦克风、定位等系统能力。拒绝授权不会影响所有功能，但对应能力可能不可用。",
            accent = Color(0xFF3367D6)
        ) {
            PermissionLine("媒体与文件", "用于本地视频、字幕弹幕、下载保存和文件选择。")
            PermissionLine("网络与通知", "用于网页访问、嗅探、远程播放、WebDAV 和任务状态。")
            PermissionLine("设备能力", "用于网页或扩展功能在用户触发时调用相机、麦克风或定位。")
        }

        AgreementSection(
            icon = Icons.Default.Gavel,
            title = "使用边界",
            body = "您需要自行确保使用行为合法合规。请勿将本应用用于侵权、盗版、绕过付费限制、批量采集、黑产或其它违法用途。",
            accent = Color(0xFF7D5A00)
        ) {
            BulletPoint("通过本应用访问、播放、下载或解析的内容，其版权和使用规则归原网站与权利人所有。")
            BulletPoint("第三方站点、账号、接口或远程服务的风控、封禁、失效和内容变化风险由用户自行判断。")
        }

        AgreementSection(
            icon = Icons.Default.WarningAmber,
            title = "免责声明",
            body = "Kiyori 是独立第三方工具，与 Bilibili、WebDAV 服务提供方及其它站点均无隶属关系。使用相关服务时，请同时遵守对应平台协议和当地法律法规。",
            accent = Color(0xFFD93025)
        ) {
            WarningNote("因自行授权、访问第三方内容、下载文件、分享内容或滥用功能产生的风险和后果，由您本人承担。")
        }

        AgreementSection(
            icon = Icons.Default.CheckCircle,
            title = "确认事项",
            body = "点击“同意并进入”即表示您已理解本页面对应用定位、权限、隐私、第三方服务与使用边界的说明。",
            accent = MaterialTheme.colorScheme.primary
        ) {
            CheckPoint("同意在合法、合规、非滥用的前提下使用 Kiyori。")
            CheckPoint("理解拒绝本协议将无法继续进入应用。")
            Text(
                text = "如果您暂时不接受以上内容，请点击“退出”。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.error,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun AgreementSection(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.10f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(17.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AgreementFooter(
    hasScrolledToBottom: Boolean,
    isChecked: Boolean,
    canConfirm: Boolean,
    confirmAlpha: Float,
    onCheckedChange: (Boolean) -> Unit,
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = !hasScrolledToBottom,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "请向下滑动阅读完整内容",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasScrolledToBottom) {
                    onCheckedChange(!isChecked)
                }
                .alpha(if (hasScrolledToBottom) 1f else 0.48f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                enabled = hasScrolledToBottom,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildAnnotatedString {
                    append("我已阅读并")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("同意")
                    }
                    append("以上内容")
                },
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.42f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "退出",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onAgree,
                enabled = canConfirm,
                modifier = Modifier
                    .weight(1.35f)
                    .height(46.dp)
                    .alpha(confirmAlpha),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFFE2E6EA),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "同意并进入",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✓",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionLine(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WarningNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFF4F2),
        border = BorderStroke(1.dp, Color(0xFFFFD8D2))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(11.dp),
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = Color(0xFF8C1D18)
        )
    }
}
