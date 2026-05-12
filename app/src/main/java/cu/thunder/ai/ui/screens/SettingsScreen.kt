package cu.thunder.ai.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.thunder.ai.R
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.ui.components.PersianTextField
import cu.thunder.ai.ui.components.SettingChangerDialog
import cu.thunder.ai.ui.components.SettingsItem
import cu.thunder.ai.ui.components.SettingsItemCard
import cu.thunder.ai.utils.DataStoreHelper
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTheme by remember {
        mutableStateOf(
            when {
                isDarkMode -> ThemeSetting.Dark
                else -> ThemeSetting.Light
            }
        )
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Usuario") }
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        DataStoreHelper.getUserName(context).collect { name ->
            if (name != null) userName = name
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil de usuario compacto
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            PersianText(
                                text = userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = {
                                    tempName = userName
                                    showNameDialog = true
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                PersianText("Cambiar nombre", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

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
                        scope.launch { DataStoreHelper.saveDarkMode(context, theme == ThemeSetting.Dark) }
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
                            text = "On Android 12+ your app theme colors are based on your home screen background",
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
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

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { PersianText("Cambiar nombre") },
            text = {
                PersianTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    placeholder = { PersianText("Tu nombre") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    userName = tempName.ifBlank { "Usuario" }
                    scope.launch { DataStoreHelper.saveUserName(context, userName) }
                    showNameDialog = false
                }) {
                    PersianText("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    PersianText("Cancelar")
                }
            }
        )
    }
}