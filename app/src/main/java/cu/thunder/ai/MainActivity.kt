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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.thunder.ai.navigation.NavRoutes
import cu.thunder.ai.ui.screens.*
import cu.thunder.ai.ui.theme.ThunderAITheme
import cu.thunder.ai.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            val chatViewModel = remember { ChatViewModel() }

            ThunderAITheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME,
                        enterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }) },
                        exitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 4 }) },
                        popEnterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { -it / 4 }) },
                        popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }) }
                    ) {
                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                onNavigateToChat = { navController.navigate(NavRoutes.chat(-1L)) },
                                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                                onNavigateToAbout = { navController.navigate(NavRoutes.ABOUT) }
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
                                onBack = { navController.popBackStack() },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
                            )
                        }

                        composable(NavRoutes.HISTORY) {
                            HistoryScreen(
                                viewModel = chatViewModel,
                                onBack = { navController.popBackStack() },
                                onChatSelected = { chatId ->
                                    navController.navigate(NavRoutes.chat(chatId)) {
                                        popUpTo(NavRoutes.HOME)
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                isDarkMode = isDarkMode,
                                onThemeChanged = { isDarkMode = it },
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