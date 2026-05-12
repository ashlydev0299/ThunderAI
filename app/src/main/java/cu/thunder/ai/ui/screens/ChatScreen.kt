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
import cu.thunder.ai.ui.components.PersianTextField
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
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.currentMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val chatTitle by viewModel.currentChatTitle.collectAsState()
    val inputEnabled by viewModel.inputEnabled.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    var chatInput by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        if (chatId != -1L) viewModel.loadChat(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollTo(listState.maxValue)
        }
    }

    BackHandler { onBack() }

    // Detección de internet
    LaunchedEffect(Unit) {
        viewModel.checkConnectivity(context)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = {
                        AnimatedContent(targetState = chatTitle) { title ->
                            PersianText(
                                text = title.ifEmpty { stringResource(R.string.new_chat) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, "Volver")
                        }
                    },
                    actions = {
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
        },
        bottomBar = {
            Crossfade(targetState = inputEnabled) { enabled ->
                if (enabled) {
                    Surface(shadowElevation = 8.dp) {
                        PersianTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { PersianText(stringResource(R.string.chat_input)) },
                            minLines = 1,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatInput.isNotBlank()) {
                                    scope.launch {
                                        viewModel.sendMessage(chatInput.trim())
                                        chatInput = ""
                                    }
                                }
                            }),
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (chatInput.isNotBlank()) ImeAction.Send else ImeAction.None,
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            leadingIcon = {
                                IconButton(onClick = { chatInput = "" }) {
                                    Icon(Icons.Outlined.Clear, stringResource(R.string.clear))
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    enabled = chatInput.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            viewModel.sendMessage(chatInput.trim())
                                            chatInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Send, stringResource(R.string.send))
                                }
                            }
                        )
                    }
                } else {
                    BottomAppBar(
                        actions = { },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { viewModel.cancelRequest() }) {
                                Icon(Icons.Outlined.Stop, stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(listState)
        ) {
            // Banner sin conexión
            AnimatedVisibility(
                visible = isOffline,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    PersianText(
                        text = stringResource(R.string.no_internet_connection),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }
            }

            // Mensajes
            messages.forEach { msg ->
                ChatBubbleItem(
                    content = msg.content,
                    owner = ChatBubbleOwner.of(msg.role),
                    isTypewriter = chatId == -1L && msg.role == "assistant",
                    isNewChat = chatId == -1L
                )
            }

            // Indicador de carga
            if (isLoading) {
                ChatBubbleItem(
                    owner = ChatBubbleOwner.Assistant,
                    content = {
                        Box(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                            AnimatedDots()
                        }
                    },
                    isTypewriter = false,
                    isNewChat = false
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
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

    ChatBubbleBox(
        owner = owner,
        onClick = { isExpanded = !isExpanded },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            clipboardManager.setText(AnnotatedString(content))
            Toast.makeText(context, stringResource(R.string.text_copied), Toast.LENGTH_SHORT).show()
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

@Composable
fun ChatBubbleItem(
    owner: ChatBubbleOwner,
    content: @Composable (ColumnScope.() -> Unit),
    isTypewriter: Boolean,
    isNewChat: Boolean
) {
    ChatBubbleBox(owner = owner, onClick = {}, onLongClick = {}, content = content)
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