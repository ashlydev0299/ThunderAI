package cu.thunder.ai.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cu.thunder.ai.R
import cu.thunder.ai.ui.animation.AnimatedDots
import cu.thunder.ai.ui.animation.TypewriterText
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.utils.DataStoreHelper
import cu.thunder.ai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

enum class ChatBubbleOwner {
    User, Assistant;
    companion object {
        fun of(value: String) = if (value.lowercase() == "user") User else Assistant
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    viewModel: ChatViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.currentMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val inputEnabled by viewModel.inputEnabled.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()

    var chatInput by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DataStoreHelper.getUserName(context).first().let { name ->
            userName = name ?: "Usuario"
        }
    }

    LaunchedEffect(chatId) {
        if (chatId != -1L) viewModel.loadChat(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollTo(listState.maxValue)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
        viewModel.checkConnectivity(context)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = {
                        Text(
                            text = "ThunderAI",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.Menu, "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.newChat() }) {
                            Icon(Icons.Outlined.Add, "Nuevo chat")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(viewModel.snackbarHost) { data ->
                PersianText(
                    text = data.visuals.message,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
            ) {
                AnimatedVisibility(
                    visible = isOffline,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WifiOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            PersianText(
                                text = stringResource(R.string.no_internet_connection),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (messages.isEmpty() && chatId == -1L) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\u26A1", fontSize = MaterialTheme.typography.displayMedium.fontSize)
                            Spacer(modifier = Modifier.height(16.dp))
                            PersianText(
                                text = "Hola, ${userName ?: "Usuario"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PersianText(
                                text = "\u00BFEn qu\u00E9 puedo ayudarte?",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                messages.forEach { msg ->
                    ChatBubbleItem(
                        content = msg.content,
                        owner = ChatBubbleOwner.of(msg.role),
                        isTypewriter = chatId == -1L && msg.role == "assistant",
                        isNewChat = chatId == -1L
                    )
                }

                if (isLoading) {
                    ChatBubbleBox(
                        owner = ChatBubbleOwner.Assistant,
                        onClick = {},
                        onLongClick = {},
                        content = {
                            Box(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                                AnimatedDots()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inputEnabled) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { PersianText(stringResource(R.string.chat_input), style = MaterialTheme.typography.bodyMedium) },
                            maxLines = 4,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (chatInput.isNotBlank()) ImeAction.Send else ImeAction.None,
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatInput.isNotBlank()) {
                                    scope.launch {
                                        viewModel.sendMessage(chatInput.trim())
                                        chatInput = ""
                                    }
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    scope.launch {
                                        viewModel.sendMessage(chatInput.trim())
                                        chatInput = ""
                                    }
                                }
                            },
                            enabled = chatInput.isNotBlank() && !isLoading,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Send,
                                stringResource(R.string.send),
                                tint = if (chatInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = { viewModel.cancelRequest() },
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Outlined.Stop, stringResource(R.string.cancel), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { PersianText("Nuevo Chat") },
                    onClick = {
                        showMenu = false
                        viewModel.newChat()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Chat, null) }
                )
                DropdownMenuItem(
                    text = { PersianText(stringResource(R.string.chat_history)) },
                    onClick = {
                        showMenu = false
                        onNavigateToHistory()
                    },
                    leadingIcon = { Icon(Icons.Outlined.History, null) }
                )
                DropdownMenuItem(
                    text = { PersianText(stringResource(R.string.settings)) },
                    onClick = {
                        showMenu = false
                        onNavigateToSettings()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Settings, null) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { PersianText(stringResource(R.string.about)) },
                    onClick = {
                        showMenu = false
                        onNavigateToSettings()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Info, null) }
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    content: String,
    owner: ChatBubbleOwner,
    isTypewriter: Boolean,
    isNewChat: Boolean
) {
    var isExpanded by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedText = stringResource(R.string.text_copied)

    ChatBubbleBox(
        owner = owner,
        onClick = { isExpanded = !isExpanded },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            clipboardManager.setText(AnnotatedString(content))
            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
        },
        content = {
            if (isTypewriter && isNewChat) {
                TypewriterText(
                    modifier = Modifier.padding(8.dp),
                    text = content.trim(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 10
                )
            } else {
                PersianText(
                    modifier = Modifier.padding(8.dp),
                    text = content.trim(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 10
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleBox(
    owner: ChatBubbleOwner,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val container = if (owner == ChatBubbleOwner.User) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val shape = if (owner == ChatBubbleOwner.User) {
        RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 2.dp, bottomStart = 16.dp)
    }

    val arrangement = if (owner == ChatBubbleOwner.User) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = container),
            shape = shape,
            content = content,
            modifier = Modifier
                .padding(4.dp)
                .clip(shape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        )
    }
}