package com.gitmob.android.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitmob.android.api.ApiClient
import com.gitmob.android.auth.AccountInfo
import com.gitmob.android.auth.AccountStore
import com.gitmob.android.auth.TokenLoginManager
import com.gitmob.android.auth.TokenLoginResult
import com.gitmob.android.auth.TokenStorage
import com.gitmob.android.ui.login.LoginUiState.*
import com.gitmob.android.util.LogManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val login: String) : LoginUiState()
    data class Error(val msg: String)     : LoginUiState()
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenStorage = TokenStorage(app)
    private val accountStore = AccountStore(app)

    private val _state = MutableStateFlow<LoginUiState>(Idle)
    val state = _state.asStateFlow()

    /** 所有已保存账号（供 LoginScreen 展示账号列表） */
    val savedAccounts: StateFlow<List<AccountInfo>> = accountStore.accounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 直接切换到已有账号 */
    fun switchToAccount(info: AccountInfo) {
        viewModelScope.launch {
            _state.value = Loading
            try {
                accountStore.switchAccount(info.login)
                tokenStorage.syncActiveAccount(info)
                ApiClient.rebuild()
                _state.value = Success(info.login)
            } catch (e: Exception) {
                _state.value = Error(e.message ?: "切换失败")
            }
        }
    }

    /** 手动输入 Token 登录 */
    fun onManualTokenReceived(token: String) {
        viewModelScope.launch {
            LogManager.d("LoginViewModel", "手动 Token 登录被触发")
            LogManager.d("LoginViewModel", "输入 Token 前缀: ${token.take(20)}...")
            _state.value = Loading
            try {
                // 先检查是否已有相同 Token 的账号
                val existingAccounts = accountStore.accounts.first()
                LogManager.d("LoginViewModel", "已有账号数量: ${existingAccounts.size}")
                val existingSameTokenAccount = existingAccounts.firstOrNull {
                    it.token == token
                }

                if (existingSameTokenAccount != null) {
                    LogManager.d("LoginViewModel", "Token 已存在，直接返回")
                    _state.value = Error("该 Token 已存在于账号列表中，请直接切换账号使用")
                    return@launch
                }

                // 验证 Token
                LogManager.d("LoginViewModel", "开始调用 TokenLoginManager.verifyToken...")
                val result = TokenLoginManager.verifyToken(token)

                when (result) {
                    is TokenLoginResult.Success -> {
                        // 直接登录
                        completeTokenLogin(
                            token = token,
                            login = result.login,
                            name = result.name,
                            email = result.email,
                            avatarUrl = result.avatarUrl
                        )
                    }
                    is TokenLoginResult.Failure -> {
                        _state.value = Error(result.message)
                    }
                    is TokenLoginResult.InsufficientScopes -> {
                        _state.value = Error("权限不全，缺少：${result.missingScopes.joinToString(", ")}")
                    }
                }
            } catch (e: Exception) {
                _state.value = Error(e.message ?: "认证失败")
            }
        }
    }

    /** 取消 Token 登录操作，回到 Idle 状态 */
    fun cancelTokenLogin() {
        _state.value = Idle
    }

    /** 完成 Token 登录的内部函数 */
    private suspend fun completeTokenLogin(
        token: String,
        login: String,
        name: String,
        email: String,
        avatarUrl: String
    ) {
        val info = AccountInfo(
            login     = login,
            name      = name,
            email     = email,
            avatarUrl = avatarUrl,
            token     = token,
            authType  = com.gitmob.android.auth.AuthType.TOKEN
        )
        accountStore.addOrUpdateAccount(info)
        tokenStorage.syncActiveAccount(info)
        ApiClient.rebuild()

        _state.value = Success(login)
    }
}
