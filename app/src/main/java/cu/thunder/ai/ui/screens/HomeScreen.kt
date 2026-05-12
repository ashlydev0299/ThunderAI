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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    icon = { Icon(Icons.Outlined.Chat, null) },
                    label = { Text("Nuevo Chat") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToChat()
                    }
                )
                NavigationDrawerItem(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    icon = { Icon(Icons.Outlined.History, null) },
                    label = { Text(stringResource(R.string.chat_history)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToHistory()
                    }
                )
                NavigationDrawerItem(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
                NavigationDrawerItem(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    icon = { Icon(Icons.Outlined.Info, null) },
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    }
                )
            }
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Surface(shadowElevation = 8.dp) {
                        TopAppBar(
                            scrollBehavior = scrollBehavior,
                            title = { PersianText("ThunderAI", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Outlined.Menu, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                                }
                                IconButton(onClick = onNavigateToAbout) {
                                    Icon(Icons.Outlined.Info, stringResource(R.string.about))
                                }
                            }
                        )
                    }
                },
                content = { paddingValues ->
                    Surface(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                content = { PersianText(stringResource(R.string.chat_with_me)) },
                                onClick = onNavigateToChat,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("\u26A1", fontSize = 64.sp)
                            Text("ThunderAI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Tu asistente de IA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
            )
        }
    )
}