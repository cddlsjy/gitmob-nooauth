GitMob Android 删除 OAuth 登录功能方法总结
一、删除的文件和目录
文件/目录路径	说明
app/src/main/java/com/gitmob/android/auth/OAuthManager.kt	OAuth 核心管理类（处理授权、撤销等）
cf-worker/ 目录	Cloudflare Worker OAuth 后端服务
二、修改的配置文件
2.1 app/build.gradle.kts - 删除 OAuth 构建配置
kotlin
// 删除以下环境变量
OAUTH_CLIENT_ID=xxx      // 删除
OAUTH_CALLBACK_URL=xxx    // 删除
OAUTH_SCOPES=xxx         // 删除

// 删除依赖
implementation(libs.androidx.browser)  // 删除
2.2 gradle/libs.versions.toml - 删除浏览器库
toml
# 删除版本定义
browser = "1.8.0"

# 删除库定义
androidx-browser = { ... }  // 删除
三、修改 AndroidManifest.xml
删除 gitmob://oauth Deep Link
xml
<!-- 删除这段 intent-filter -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="gitmob" android:host="oauth" />
</intent-filter>
四、修改 MainActivity.kt
删除 OAuth Deep Link 处理
kotlin
// 删除前
private fun handleDeepLink(intent: Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme == "gitmob" && uri.host == "oauth") {
        // 处理 OAuth callback
    }
    // ...
}

// 简化后：只保留 GitHub URL 处理
private fun handleDeepLink(intent: Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme == "https" && uri.host == "github.com") {
        pendingGitHubUrl = uri.toString()
    }
}
五、修改 LoginScreen.kt
核心修改点
1.
删除 OAuth 登录按钮 - 移除 GitHub 授权按钮 UI
2.
删除 OAuth 相关弹窗 - 移除授权确认对话框
3.
保留 Token 登录 - 保留手动输入 Token 的 UI
4.
保留多账号切换 - 保留账号选择器功能
六、修改 LoginViewModel.kt
删除的状态类（LoginUiState）
kotlin
// 删除以下状态
data class ConfirmReplaceOAuth(...)    // 删除
data class ConfirmReplaceToken(...)    // 删除（OAuth 登录时使用）
data class ConfirmReplaceOldOAuth(...) // 删除
删除的方法
方法名	说明
onOAuthError()	OAuth 错误处理
onTokenReceived()	OAuth callback token 处理
completeOAuthLogin()	完成 OAuth 登录
confirmUseOAuthKeepToken()	OAuth 登录确认（保留 Token）
keepTokenAndRevokeOAuth()	撤销 OAuth
confirmUseNewOAuth()	新旧 OAuth 切换
keepOldOAuthAndRevokeNew()	保留旧 OAuth
confirmReplaceOAuth()	替换 OAuth 账号
skipRevokeAndLogin()	跳过撤销登录
保留的方法
方法名	说明
switchToAccount()	切换已有账号
onManualTokenReceived()	Token 登录
cancelTokenLogin()	取消登录
completeTokenLogin()	完成 Token 登录
七、修改 SettingsScreen.kt
删除的 UI 选项
kotlin
// 删除 "取消所有授权" 选项
SRow(
    title = "取消所有授权",
    subtitle = "彻底移除 GitMob 的 GitHub 授权",
    // ...
)

// 删除 "取消授权确认弹窗" 整个 Dialog
简化账号操作
kotlin
// 原来根据账号类型显示不同选项
if (activeAccount?.authType == AuthType.TOKEN) {
    // Token 账号：只显示移除账号
} else {
    // OAuth 账号：显示退出登录和取消授权
}

// 简化为：所有账号只显示移除账号
SRow(title = "移除账号", ...)
八、修改 NavGraph.kt
删除参数
kotlin
// AppNavGraph 函数签名修改
// 删除前
fun AppNavGraph(
    tokenStorage: TokenStorage,
    onThemeChange: (ThemeMode) -> Unit,
    initialToken: String?,        // 删除
    onTokenConsumed: () -> Unit, // 删除
    // ...
)

// 删除后
fun AppNavGraph(
    tokenStorage: TokenStorage,
    onThemeChange: (ThemeMode) -> Unit,
    // ...
)
九、AccountStore 相关处理
虽然 AccountStore.kt 和 TokenStorage.kt 保留了 AuthType 枚举（用于标识已存在账号的类型），但登录流程不再创建 OAuth 类型的新账号。

AuthType 枚举保留说明
kotlin
enum class AuthType {
    OAUTH,  // 保留，标记已存在的 OAuth 账号
    TOKEN   // 保留，用于新登录的 Token 账号
}
十、验证清单
完成修改后，确保：

检查项	验证方法
OAuth 代码已删除	grep -r "OAuth" src/ 应无结果
OAuthManager 已删除	文件不存在
cf-worker 已删除	目录不存在
browser 库已删除	grep "browser" build.gradle.kts 无结果
gitmob://oauth intent-filter 已删除	grep "gitmob" AndroidManifest.xml 无 oauth 相关
Token 登录可用	编译验证
多账号切换可用	编译验证
十一、输出文件
修改后的完整源码已打包为：
