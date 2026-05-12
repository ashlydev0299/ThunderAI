package cu.thunder.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.thunder.ai.R
import cu.thunder.ai.ui.components.PersianText
import kotlinx.coroutines.launch

enum class NavigationItem(val labelId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home(R.string.home, Icons.Outlined.Home),
    NewChat(R.string.new_chat, Icons.AutoMirrored.Outlined.Chat),
    History(R.string.chat_history, Icons.Outlined.History),
    Settings(R.string.settings, Icons.Outlined.Settings),
    About(R.string.about, Icons.Outlined.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedItem by remember { mutableStateOf(NavigationItem.Home) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val navigateTo: (NavigationItem) -> Unit = { item ->
        when (item) {
            NavigationItem.NewChat -> onNavigateToChat()
            NavigationItem.History -> onNavigateToHistory()
            NavigationItem.Settings -> onNavigateToSettings()
            NavigationItem.About -> onNavigateToAbout()
            else -> {}
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationItem.entries.forEach { item ->
                    NavigationDrawerItem(
                        modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelId)) },
                        label = { PersianText(stringResource(item.labelId)) },
                        selected = selectedItem == item,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem = item
                            navigateTo(item)
                        }
                    )
                }
            }
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Surface(shadowElevation = 8.dp) {
                        TopAppBar(
                            scrollBehavior = scrollBehavior,
                            title = { PersianText(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Outlined.Menu, contentDescription = null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { navigateTo(NavigationItem.Settings) }) {
                                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                                }
                                IconButton(onClick = { navigateTo(NavigationItem.About) }) {
                                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about))
                                }
                            }
                        )
                    }
                },
                content = { paddingValues ->
                    Surface(
                        modifier = Modifier.padding(paddingValues).padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onNavigateToChat() },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                PersianText(stringResource(R.string.chat_with_me))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "\u26A1",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "ThunderAI",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tu asistente de IA",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            )
        }
    )
}