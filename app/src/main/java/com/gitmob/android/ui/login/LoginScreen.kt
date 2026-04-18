package com.gitmob.android.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.gitmob.android.R
import com.gitmob.android.auth.AccountInfo
import com.gitmob.android.auth.AuthType
import com.gitmob.android.ui.theme.*

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    val state   by vm.state.collectAsState()
    val savedAccounts by vm.savedAccounts.collectAsState()
    var showTokenLoginDialog by remember { mutableStateOf(false) }

    // 成功后跳转
    LaunchedEffect(state) {
        if (state is LoginUiState.Success) onSuccess()
    }

    // 只有在 Idle 或 Loading 状态且有已保存账号时才显示账号选择页
    // 避免登录成功前 savedAccounts 变化导致闪一下
    val showAccountPicker = savedAccounts.isNotEmpty() &&
        (state is LoginUiState.Idle || state is LoginUiState.Loading)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (showAccountPicker) {
            // ── 账号选择页 ──────────────────────────────────────────
            AccountPickerContent(
                accounts      = savedAccounts,
                state         = state,
                onSelectAccount = { vm.switchToAccount(it) },
                onTokenLogin  = { showTokenLoginDialog = true },
            )
        } else {
            // ── 全新登录页 ──────────────────────────────────────────
            FreshLoginContent(
                state          = state,
                onTokenLogin   = { showTokenLoginDialog = true },
            )
        }

        // Token 登录弹窗
        if (showTokenLoginDialog) {
            TokenLoginDialog(
                onDismiss     = { showTokenLoginDialog = false },
                onTokenSubmit = { token ->
                    vm.onManualTokenReceived(token)
                    showTokenLoginDialog = false
                },
                state         = state,
            )
        }
    }
}

// ── 账号选择器（有已保存账号时）──────────────────────────────────────

@Composable
private fun AccountPickerContent(
    accounts: List<AccountInfo>,
    state: LoginUiState,
    onSelectAccount: (AccountInfo) -> Unit,
    onTokenLogin: () -> Unit,
) {
    val c = LocalGmColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Spacer(Modifier.height(32.dp))

        // Logo + 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp),
            )
            Column {
                Text(
                    "GitMob",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "选择登录账号",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 状态提示（加载/错误）
        AnimatedVisibility(visible = state is LoginUiState.Loading || state is LoginUiState.Error) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                when (state) {
                    is LoginUiState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.bgCard, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Coral,
                        )
                        Text("正在切换账号…", fontSize = 13.sp, color = c.textSecondary)
                    }
                    is LoginUiState.Error -> Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            state.msg,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                    else -> Unit
                }
            }
        }

        // 账号列表
        Text(
            "已授权账号",
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = c.textTertiary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .background(c.bgCard, RoundedCornerShape(16.dp)),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(accounts, key = { it.login }) { account ->
                AccountItemRow(
                    account    = account,
                    isLoading  = state is LoginUiState.Loading,
                    onClick    = { onSelectAccount(account) },
                    c          = c,
                )
                if (account != accounts.last()) {
                    HorizontalDivider(
                        color = c.border, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Token 登录按钮
        OutlinedButton(
            onClick  = onTokenLogin,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("使用 Token 登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
        }

    }
}

@Composable
private fun AccountItemRow(
    account: AccountInfo,
    isLoading: Boolean,
    onClick: () -> Unit,
    c: GmColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 头像
        AsyncImage(
            model = account.avatarUrl.let { if (it.contains("?")) it else "$it?s=80" },
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.bgItem),
        )
        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    account.displayName,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                )
                if (account.authType == AuthType.TOKEN) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "Token",
                            fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                "@${account.login}",
                fontSize = 12.sp, color = c.textSecondary,
            )
        }
        // 箭头
        Icon(
            Icons.Default.ChevronRight, null,
            tint = c.textTertiary, modifier = Modifier.size(20.dp),
        )
    }
}

// ── 全新登录页（无已保存账号）────────────────────────────────────────

@Composable
private fun FreshLoginContent(
    state: LoginUiState,
    onTokenLogin: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "GitMob", fontSize = 36.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "手机端 GitHub 管理工具", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))

        when (val s = state) {
            is LoginUiState.Loading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("正在验证…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            is LoginUiState.Error -> {
                val is401 = s.msg.contains("401") || s.msg.contains("token") || s.msg.contains("失效")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (is401) "授权已失效" else "登录失败",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (is401) "你的访问令牌已被撤销或过期，请重新授权登录。" else s.msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp, lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onTokenLogin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                ) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("使用 Token 登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            else -> {
                Spacer(Modifier.height(48.dp))
                OutlinedButton(
                    onClick = onTokenLogin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                ) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("使用 Token 登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "使用 GitHub Personal Access Token 登录\n权限：repo · workflow · user · notifications · delete_repo",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center, lineHeight = 17.sp,
        )
    }
}

// ── Token 登录弹窗 ─────────────────────────────────────────
@Composable
private fun TokenLoginDialog(
    onDismiss: () -> Unit,
    onTokenSubmit: (String) -> Unit,
    state: LoginUiState,
) {
    var tokenInput by remember { mutableStateOf("") }
    val isLoading = state is LoginUiState.Loading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            Button(
                onClick = { onTokenSubmit(tokenInput) },
                enabled = tokenInput.isNotBlank() && !isLoading,
            ) {
                Text("登录")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text("取消")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Key, contentDescription = null)
                Text("使用 Token 登录")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "请输入 GitHub Personal Access Token (PAT)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Token") },
                    placeholder = { Text("ghp_...") },
                    singleLine = true,
                    enabled = !isLoading,
                )
                if (state is LoginUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            state.msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Text(
                    "所需权限：repo, workflow, user, notifications, delete_repo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}


