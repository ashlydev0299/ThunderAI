package cu.thunder.ai.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
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
import cu.thunder.ai.utils.DataStoreHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDark by remember { mutableStateOf(isDarkMode) }
    var userName by remember { mutableStateOf("Usuario") }
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var fontSize by remember { mutableIntStateOf(14) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFontSizeSelector by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var inAppNotifications by remember { mutableStateOf(true) }
    var popupNotifications by remember { mutableStateOf(false) }
    var proactiveMessages by remember { mutableStateOf(true) }
    var locationAccess by remember { mutableStateOf(false) }

    // Cargar valores guardados
    LaunchedEffect(Unit) {
        DataStoreHelper.getFontSize(context).collect { fontSize = it }
        DataStoreHelper.getUserName(context).collect { userName = it }
        DataStoreHelper.getNotificationsEnabled(context).collect { notificationsEnabled = it }
        DataStoreHelper.getInAppNotifications(context).collect { inAppNotifications = it }
        DataStoreHelper.getPopupNotifications(context).collect { popupNotifications = it }
        DataStoreHelper.getProactiveMessages(context).collect { proactiveMessages = it }
        DataStoreHelper.getLocationAccess(context).collect { locationAccess = it }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = { PersianText("Perfil", fontWeight = FontWeight.Bold) },
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
            // Foto de perfil centrada sin tarjeta
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PersianText(text = userName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ========== CUENTA ==========
            item { CategoryHeader("Cuenta") }

            item {
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    title = "Nombre de Usuario",
                    subtitle = userName,
                    onClick = { tempName = userName; showNameDialog = true }
                )
            }

            item { SubCategoryHeader("Permisos") }
            item {
                SwitchRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notificaciones",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it; scope.launch { DataStoreHelper.saveNotificationsEnabled(context, it) } }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Acceso a la ubicación",
                    subtitle = "Permite mejor precisión de contenido",
                    checked = locationAccess,
                    onCheckedChange = { locationAccess = it; scope.launch { DataStoreHelper.saveLocationAccess(context, it) } }
                )
            }

            item { SubCategoryHeader("Privacidad") }
            item {
                SwitchRow(
                    icon = Icons.Outlined.Mail,
                    title = "Proactividad",
                    subtitle = "ThunderAI puede enviarte mensajes proactivos",
                    checked = proactiveMessages,
                    onCheckedChange = { proactiveMessages = it; scope.launch { DataStoreHelper.saveProactiveMessages(context, it) } }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Borrar el Historial de chat",
                    onClick = { showClearDialog = true }
                )
            }

            // ========== APARIENCIA ==========
            item { CategoryHeader("Apariencia") }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = "Tema",
                    subtitle = if (selectedDark) "Oscuro" else "Claro",
                    onClick = { showThemeSelector = true }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.FormatSize,
                    title = "Tamaño de fuente",
                    subtitle = "$fontSize sp",
                    onClick = { showFontSizeSelector = true }
                )
            }

            // ========== NOTIFICACIONES ==========
            item { CategoryHeader("Notificaciones") }
            item {
                SwitchRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Todas las Notificaciones",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it; scope.launch { DataStoreHelper.saveNotificationsEnabled(context, it) } }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Notificaciones en la aplicación",
                    checked = inAppNotifications,
                    onCheckedChange = { inAppNotifications = it; scope.launch { DataStoreHelper.saveInAppNotifications(context, it) } }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Outlined.FilterList,
                    title = "Notificaciones emergentes",
                    checked = popupNotifications,
                    onCheckedChange = { popupNotifications = it; scope.launch { DataStoreHelper.savePopupNotifications(context, it) } }
                )
            }

            // ========== CONFIGURACIÓN DE VOZ ==========
            item { CategoryHeader("Configuración de voz") }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Mic,
                    title = "Entrada de voz",
                    subtitle = "Usa el micrófono en el chat",
                    onClick = { onBack() }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Salida de voz",
                    subtitle = "Próximamente - Texto a voz",
                    onClick = { }
                )
            }

            // ========== CONTACTO ==========
            item { CategoryHeader("Contacto") }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Mail,
                    title = "Contactar al soporte",
                    subtitle = "ashlydev99@gmail.com",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:ashlydev99@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Soporte ThunderAI")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // ========== ACERCA DE ==========
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Acerca de",
                    onClick = onNavigateToAbout
                )
            }

            item {
                Text(
                    text = "v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }

    // Selector de tema
    if (showThemeSelector) {
        AlertDialog(
            onDismissRequest = { showThemeSelector = false },
            title = { PersianText(stringResource(R.string.theme)) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !selectedDark, onClick = { selectedDark = false; onThemeChanged(false); scope.launch { DataStoreHelper.saveDarkMode(context, false) }; showThemeSelector = false }, label = { PersianText(stringResource(R.string.theme_light)) })
                        FilterChip(selected = selectedDark, onClick = { selectedDark = true; onThemeChanged(true); scope.launch { DataStoreHelper.saveDarkMode(context, true) }; showThemeSelector = false }, label = { PersianText(stringResource(R.string.theme_dark)) })
                    }
                }
            },
            confirmButton = { },
            dismissButton = { TextButton(onClick = { showThemeSelector = false }) { PersianText("Cancelar") } }
        )
    }

    // Selector de tamaño de fuente
    if (showFontSizeSelector) {
        AlertDialog(
            onDismissRequest = { showFontSizeSelector = false },
            title = { PersianText("Tamaño de fuente") },
            text = {
                Column {
                    PersianText("$fontSize sp", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() }, valueRange = 10f..22f, steps = 5)
                }
            },
            confirmButton = { Button(onClick = { scope.launch { DataStoreHelper.saveFontSize(context, fontSize) }; showFontSizeSelector = false }) { PersianText("Guardar") } },
            dismissButton = { TextButton(onClick = { showFontSizeSelector = false }) { PersianText("Cancelar") } }
        )
    }

    // Diálogo cambiar nombre
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

    // Diálogo borrar historial
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { PersianText("Borrar historial") },
            text = { PersianText("¿Estás seguro de borrar todo el historial de chat?") },
            confirmButton = { Button(onClick = { onClearHistory(); showClearDialog = false }) { PersianText("Borrar") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { PersianText("Cancelar") } }
        )
    }
}

// ========== COMPONENTES ==========

@Composable
fun CategoryHeader(title: String) {
    PersianText(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
fun SubCategoryHeader(title: String) {
    PersianText(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
    )
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                PersianText(title, fontSize = 14.sp)
                if (subtitle != null) {
                    PersianText(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                PersianText(title, fontSize = 14.sp)
                if (subtitle != null) {
                    PersianText(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}