package cu.thunder.ai.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.thunder.ai.R
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.ui.components.PersianTextField
import cu.thunder.ai.utils.DataStoreHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDark by remember { mutableStateOf(isDarkMode) }
    var userName by remember { mutableStateOf("Usuario") }
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    // Tamaño de fuente
    var fontSize by remember { mutableStateOf(14) }

    LaunchedEffect(Unit) {
        DataStoreHelper.getFontSize(context).collect { size -> fontSize = size }
        DataStoreHelper.getUserName(context).collect { name -> if (name != null) userName = name }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = { PersianText(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Volver") }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil compacto
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(44.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Person, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            PersianText(text = userName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            TextButton(onClick = { tempName = userName; showNameDialog = true }, contentPadding = PaddingValues(0.dp)) {
                                PersianText("Cambiar nombre", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Tema (solo Claro/Oscuro)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PersianText(stringResource(R.string.theme), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !selectedDark,
                                onClick = {
                                    selectedDark = false
                                    onThemeChanged(false)
                                    scope.launch { DataStoreHelper.saveDarkMode(context, false) }
                                },
                                label = { PersianText(stringResource(R.string.theme_light)) }
                            )
                            FilterChip(
                                selected = selectedDark,
                                onClick = {
                                    selectedDark = true
                                    onThemeChanged(true)
                                    scope.launch { DataStoreHelper.saveDarkMode(context, true) }
                                },
                                label = { PersianText(stringResource(R.string.theme_dark)) }
                            )
                        }
                    }
                }
            }

            // Tamaño de fuente
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PersianText("Tamaño de fuente", fontWeight = FontWeight.Bold)
                            PersianText("$fontSize sp", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = {
                                fontSize = it.toInt()
                                scope.launch { DataStoreHelper.saveFontSize(context, it.toInt()) }
                            },
                            valueRange = 10f..22f,
                            steps = 5
                        )
                    }
                }
            }

            // Información
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PersianText("ThunderAI v1.0")
                        Spacer(modifier = Modifier.height(4.dp))
                        PersianText(stringResource(R.string.about_app), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { PersianText("Cambiar nombre") },
            text = { PersianTextField(value = tempName, onValueChange = { tempName = it }, singleLine = true, placeholder = { PersianText("Tu nombre") }) },
            confirmButton = {
                Button(onClick = {
                    userName = tempName.ifBlank { "Usuario" }
                    scope.launch { DataStoreHelper.saveUserName(context, userName) }
                    showNameDialog = false
                }) { PersianText("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { PersianText("Cancelar") } }
        )
    }
}