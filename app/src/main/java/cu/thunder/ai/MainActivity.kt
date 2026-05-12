package cu.thunder.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cu.thunder.ai.navigation.NavRoutes
import cu.thunder.ai.ui.screens.*
import cu.thunder.ai.ui.theme.ThunderAITheme
import cu.thunder.ai.utils.DataStoreHelper
import cu.thunder.ai.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current

            var isDarkMode by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                DataStoreHelper.getDarkMode(context).collect { dark ->
                    isDarkMode = dark
                }
            }

            val chatViewModel = remember { ChatViewModel() }

            ThunderAITheme(darkTheme = isDarkMode) {
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
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
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

                        composable(NavRoutes.CHAT) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: -1L
                            ChatScreen(
                                chatId = chatId,
                                viewModel = chatViewModel,
                                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                isDarkMode = isDarkMode,
                                onThemeChanged = { dark ->
                                    isDarkMode = dark
                                    kotlinx.coroutines.MainScope().launch {
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