package cu.thunder.ai.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cu.thunder.ai.R
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.ui.components.SettingChangerDialog
import cu.thunder.ai.ui.components.SettingsItem
import cu.thunder.ai.ui.components.SettingsItemCard

enum class ThemeSetting(val labelResId: Int) {
    System(R.string.theme_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedTheme by remember {
        mutableStateOf(
            when {
                isDarkMode -> ThemeSetting.Dark
                else -> ThemeSetting.Light
            }
        )
    }
    var selectedModel by remember { mutableStateOf("liquid/lfm-2.5-1.2b-instruct:free") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val availableModels = listOf(
        "liquid/lfm-2.5-1.2b-instruct:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-r1:free"
    )

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = { PersianText(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, "Volver")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tema
            item {
                SettingChangerDialog(
                    isEnabled = showThemeDialog,
                    title = stringResource(R.string.theme),
                    options = ThemeSetting.entries.toList(),
                    currentSetting = selectedTheme,
                    onDismiss = { showThemeDialog = false },
                    displayProvider = { context.getString(it.labelResId) },
                    onSettingChange = { theme ->
                        selectedTheme = theme
                        onThemeChanged(theme == ThemeSetting.Dark)
                    },
                    icon = { Icon(Icons.Outlined.DisplaySettings, stringResource(R.string.theme)) }
                )

                SettingsItemCard(title = stringResource(R.string.theme)) {
                    SettingsItem(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Outlined.DisplaySettings, stringResource(R.string.theme))
                        PersianText(stringResource(selectedTheme.labelResId))
                    }
                    if (selectedTheme == ThemeSetting.System && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PersianText(
                            text = stringResource(R.string.dynamic_theme_notice),
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Modelo IA
            item {
                SettingChangerDialog(
                    isEnabled = showModelDialog,
                    title = stringResource(R.string.api_model),
                    options = availableModels,
                    currentSetting = selectedModel,
                    onDismiss = { showModelDialog = false },
                    displayProvider = { it },
                    onSettingChange = { selectedModel = it },
                    icon = { Icon(Icons.Outlined.Dataset, stringResource(R.string.api_model)) }
                )

                SettingsItemCard(title = stringResource(R.string.api_model)) {
                    SettingsItem(onClick = { showModelDialog = true }) {
                        Icon(Icons.Outlined.Dataset, stringResource(R.string.api_model))
                        PersianText(selectedModel)
                    }
                    Button(
                        onClick = { selectedModel = availableModels.first() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PersianText(stringResource(R.string.change_it_to_default))
                    }
                }
            }

            // Información
            item {
                SettingsItemCard(title = stringResource(R.string.about)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        PersianText("ThunderAI v1.0")
                        Spacer(modifier = Modifier.height(4.dp))
                        PersianText(
                            text = stringResource(R.string.about_app),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}