package com.gitmob.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.gitmob.android.auth.ThemeMode
import com.gitmob.android.auth.TokenStorage
import com.gitmob.android.ui.nav.AppNavGraph
import com.gitmob.android.ui.theme.GitMobTheme
import com.gitmob.android.util.GitHubUrlParser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenStorage: TokenStorage

    /** 从其他 App 传入的 github.com 链接，由 NavGraph 解析跳转 */
    private var pendingGitHubUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tokenStorage = (application as GitMobApp).tokenStorage

        var keepSplashOnScreen by mutableStateOf(true)

        splash.setKeepOnScreenCondition { keepSplashOnScreen }

        // 处理冷启动时就带着 github.com 链接的情况
        handleDeepLink(intent)

        setContent {
            val themeMode by tokenStorage.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accessToken by tokenStorage.accessToken.collectAsState(initial = null)
            
            LaunchedEffect(accessToken) {
                keepSplashOnScreen = false
            }
            
            GitMobTheme(themeMode = themeMode) {
                AppNavGraph(
                    tokenStorage  = tokenStorage,
                    onThemeChange = { mode ->
                        lifecycleScope.launch { tokenStorage.setThemeMode(mode) }
                    },
                    initialGitHubUrl = pendingGitHubUrl,
                    onGitHubUrlConsumed = { pendingGitHubUrl = null },
                )
            }
        }
    }

    /** 处理从其他 App 传入的 github.com 链接 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "https" && uri.host == "github.com") {
            pendingGitHubUrl = uri.toString()
        }
    }
}
