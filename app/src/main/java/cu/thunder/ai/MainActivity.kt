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

    private var voiceResultCallback: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun startVoiceRecognition(callback: (String) -> Unit) {
        voiceResultCallback = callback
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().language)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
        }
        try { startActivityForResult(intent, 123) }
        catch (e: Exception) { voiceResultCallback?.invoke("") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            voiceResultCallback?.invoke(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            var isDarkMode by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                DataStoreHelper.getDarkMode(context).collect { dark -> isDarkMode = dark }
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
                                    scope.launch { DataStoreHelper.saveDarkMode(context, dark) }
                                },
                                onBack = { navController.popBackStack() },
                                onNavigateToAbout = { navController.navigate(NavRoutes.ABOUT) },
                                onClearHistory = { chatViewModel.deleteAllChats() }
                            )
                        }

                        composable(NavRoutes.ABOUT) {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}