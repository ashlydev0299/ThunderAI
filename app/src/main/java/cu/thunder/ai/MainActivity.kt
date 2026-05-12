package cu.thunder.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.thunder.ai.navigation.NavRoutes
import cu.thunder.ai.ui.screens.*
import cu.thunder.ai.ui.theme.ThunderAITheme
import cu.thunder.ai.utils.DataStoreHelper
import cu.thunder.ai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, app continues */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current

            var isDarkMode by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                DataStoreHelper.getDarkMode(context).collect { dark ->
                    isDarkMode = dark
                }
            }

            if (isDarkMode == null) {
                Box(modifier = Modifier.fillMaxSize())
                return@setContent
            }

            val chatViewModel = remember { ChatViewModel() }

            ThunderAITheme(darkTheme = isDarkMode!!) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.CHAT_MAIN,
                        enterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }) },
                        exitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 4 }) },
                        popEnterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { -it / 4 }) },
                        popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }) }
                    ) {
                        composable(NavRoutes.CHAT_MAIN) {
                            ChatScreen(
                                chatId = -1L,
                                viewModel = chatViewModel,
                                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                                onNavigateToAbout = { navController.navigate(NavRoutes.ABOUT) }
                            )
                        }

                        composable(NavRoutes.HISTORY) {
                            HistoryScreen(
                                viewModel = chatViewModel,
                                onBack = { navController.popBackStack() },
                                onChatSelected = { chatId ->
                                    navController.navigate(NavRoutes.chat(chatId)) {
                                        popUpTo(NavRoutes.CHAT_MAIN)
                                    }
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.CHAT,
                            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getLong("chatId") ?: -1L
                            ChatScreen(
                                chatId = chatId,
                                viewModel = chatViewModel,
                                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                                onNavigateToAbout = { navController.navigate(NavRoutes.ABOUT) }
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            val scope = rememberCoroutineScope()
                            SettingsScreen(
                                isDarkMode = isDarkMode!!,
                                onThemeChanged = { dark ->
                                    isDarkMode = dark
                                    scope.launch {
                                        DataStoreHelper.saveDarkMode(context, dark)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.ABOUT) {
                            AboutScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}