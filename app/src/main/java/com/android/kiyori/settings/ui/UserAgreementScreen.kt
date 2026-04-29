package com.android.kiyori.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun UserAgreementScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var isAutoSnapping by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isChecked && hasScrolledToBottom) 1f else 0.55f,
        animationSpec = tween(220),
        label = "agreement_button_alpha"
    )

    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 32) {
            hasScrolledToBottom = true
        }
    }

    LaunchedEffect(scrollState, viewportHeightPx) {
        if (viewportHeightPx <= 0) return@LaunchedEffect

        snapshotFlow { scrollState.isScrollInProgress }
            .map { inProgress -> !inProgress }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest {
                if (isAutoSnapping || scrollState.maxValue <= 0) return@collectLatest

                val pageHeight = viewportHeightPx.coerceAtLeast(1)
                val current = scrollState.value
                val max = scrollState.maxValue
                val nearBottom = max - current < pageHeight / 2
                val target = when {
                    nearBottom -> max
                    current < pageHeight / 2 -> 0
                    else -> ((current.toFloat() / pageHeight).roundToInt() * pageHeight)
                        .coerceIn(0, max)
                }

                if (abs(target - current) < 24) return@collectLatest

                isAutoSnapping = true
                try {
                    scrollState.animateScrollTo(
                        value = target,
                        animationSpec = tween(durationMillis = 110)
                    )
                } finally {
                    isAutoSnapping = false
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { viewportHeightPx = it.height },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, outlineColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AgreementTitleBlock()
                    Divider(color = outlineColor)
                    AgreementContent()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = !hasScrolledToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "请先滑动到最底部，再进行确认",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = hasScrolledToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                                append("以上协议内容")
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = "退出",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onAgree,
                            enabled = isChecked,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .alpha(buttonAlpha),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "同意并进入",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgreementTitleBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "FIRST LAUNCH AGREEMENT",
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Kiyori 使用协议与隐私说明",
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "请在首次进入应用前阅读以下内容，确认应用定位、权限调用方式、数据处理边界以及第三方服务相关风险。",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "最后更新：2026年4月28日",
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun AgreementContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AgreementSection(
            icon = Icons.Default.Info,
            title = "一、应用定位",
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            BulletPoint("Kiyori 以内置浏览器为主入口，提供网页访问、视频嗅探、远程播放、本地媒体浏览与播放器能力。")
            BulletPoint("当前版本同时包含 WebDAV、字幕、弹幕、下载、播放历史、Anime4K 与 Bilibili 相关扩展模块。")
            BulletPoint("本应用免费开源，遵守 GPL-3.0-or-later 协议。")
            BulletPoint("部分功能依赖第三方网站、账号状态、网络环境和远程服务可用性，实际效果可能随外部环境变化。")
        }

        AgreementSection(
            icon = Icons.Default.Security,
            title = "二、数据与隐私",
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            SubTitle("【本地存储】")
            CheckPoint("应用不提供自建账号体系，也不会将常规使用数据上传到 Kiyori 自有服务器。")
            CheckPoint("书签、历史记录、播放进度、下载配置、字幕或弹幕关联等数据默认保存在本机。")
            CheckPoint("如您使用 WebDAV、远程链接输入或网页登录，相关地址、凭证或会话仅用于对应功能。")

            Spacer(modifier = Modifier.height(4.dp))

            SubTitle("【敏感信息保护】")
            BulletPoint("Bilibili 登录凭证等敏感信息会优先加密保存在本地设备。")
            BulletPoint("相关密钥由 Android KeyStore 保护，应用不会主动导出这些数据。")
            BulletPoint("卸载应用后，应用私有目录中的本地数据通常会被系统一并清理。")
        }

        AgreementSection(
            icon = Icons.Default.Warning,
            title = "三、权限与设备能力",
            iconTint = MaterialTheme.colorScheme.secondary
        ) {
            SubTitle("【首次启动可能申请】")
            Text(
                text = "当前版本会在进入主界面后按系统流程申请媒体、文件、通知及部分设备能力权限，以兼容浏览器与媒体模块。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PermissionItem("媒体与文件访问", "用于扫描本地视频、读取媒体文件、导入或保存字幕弹幕、管理下载内容。")
            PermissionItem("网络与通知", "用于网页访问、视频嗅探、远程播放、WebDAV、下载任务和状态提醒。")
            PermissionItem("摄像头、麦克风、定位等设备能力", "用于兼容网页或扩展功能在需要时调用相应系统能力。")
            PermissionItem("电话、短信、附近设备等兼容权限", "用于兼容浏览器或网页场景可能触发的系统能力，不需要时可拒绝。")
            PermissionItem("安装未知应用、修改系统设置等特殊权限", "用于安装下载的 APK 或完成播放器相关系统设置。")

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "您可以按需授权；拒绝后，对应功能可能不可用，但授权状态可在系统设置中随时修改。",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }

        AgreementSection(
            icon = Icons.Default.Warning,
            title = "四、使用边界与免责声明",
            iconTint = Color(0xFFFF6B6B)
        ) {
            WarningCard {
                SubTitle("【请勿滥用】")
                Spacer(modifier = Modifier.height(2.dp))
                BulletPoint("请勿将本应用用于侵权、盗版、绕过付费限制、批量采集、黑产或其他违法用途。")
                BulletPoint("通过本应用访问、播放、下载或解析的内容，其版权与使用规则归原网站和原权利人所有。")
                BulletPoint("第三方站点、账号、接口或远程服务的封禁、失效、风控或内容变化风险需由用户自行判断。")
            }

            Spacer(modifier = Modifier.height(4.dp))

            SubTitle("【第三方关系说明】")
            BulletPoint("本应用是独立的第三方工具，与 Bilibili、WebDAV 服务提供方及其他站点均无隶属关系。")
            BulletPoint("如您使用相关服务，请同时遵守对应平台的用户协议、版权要求和当地法律法规。")

            Spacer(modifier = Modifier.height(4.dp))

            SubTitle("【责任边界】")
            BulletPoint("因您自行授权、访问第三方内容、下载文件或进行分享而产生的风险和后果，由您本人承担。")
            BulletPoint("开发者不对第三方内容的合法性、稳定性、可用性或由滥用功能造成的损失负责。")
        }

        AgreementSection(
            icon = Icons.Default.CheckCircle,
            title = "五、用户确认",
            iconTint = MaterialTheme.colorScheme.tertiary
        ) {
            Text(
                text = "点击“同意并进入”即表示您：",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            CheckPoint("已阅读并理解本页面对应用定位、权限、隐私和使用边界的说明。")
            CheckPoint("同意在合法、合规、非滥用的前提下使用 Kiyori。")
            CheckPoint("理解部分功能依赖第三方服务与系统授权，相关风险需自行判断和承担。")
            CheckPoint("知悉拒绝本协议将无法继续进入应用。")

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "如果您暂时不接受以上内容，请点击“退出”。",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.error,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun AgreementSection(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "•",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CheckPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✓",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PermissionItem(permission: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "•",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = permission,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun WarningCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFFF6B6B).copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}
