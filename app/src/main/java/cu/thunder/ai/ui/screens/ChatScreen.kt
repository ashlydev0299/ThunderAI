package cu.thunder.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.thunder.ai.R
import cu.thunder.ai.data.local.ChatEntity
import cu.thunder.ai.ui.animation.AnimatedDots
import cu.thunder.ai.ui.animation.TypewriterText
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.utils.DataStoreHelper
import cu.thunder.ai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val messages by viewModel.currentMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val inputEnabled by viewModel.inputEnabled.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var chatInput by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf<String?>(null) }
    var showBubbleMenu by remember { mutableStateOf<Int?>(null) }
    var showLikeFeedback by remember { mutableStateOf<String?>(null) }
    var showDeleteMenu by remember { mutableStateOf<Long?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedChats by remember { mutableStateOf(setOf<Long>()) }
    var replyTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }

    var fontSize by remember { mutableStateOf(14) }
    LaunchedEffect(Unit) {
        DataStoreHelper.getFontSize(context).collect { size -> fontSize = size }
        DataStoreHelper.getUserName(context).collect { name -> userName = name ?: "Usuario" }
    }

    LaunchedEffect(chatId) {
        if (chatId != -1L) viewModel.loadChat(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollTo(listState.maxValue)
    }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
        viewModel.checkConnectivity(context)
    }

    LaunchedEffect(showLikeFeedback) {
        showLikeFeedback?.let { feedback ->
            viewModel.sendMessage(feedback)
            showLikeFeedback = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.History, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Historial de chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    if (allChats.isNotEmpty()) {
                        IconButton(onClick = { isSelectionMode = !isSelectionMode; selectedChats = emptySet() }, modifier = Modifier.size(32.dp)) {
                            Icon(if (isSelectionMode) Icons.Outlined.Close else Icons.Outlined.Checklist, if (isSelectionMode) "Cancelar" else "Seleccionar", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                HorizontalDivider()
                if (isSelectionMode && selectedChats.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${selectedChats.size} seleccionados", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { selectedChats.forEach { viewModel.deleteChat(it) }; isSelectionMode = false; selectedChats = emptySet() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    }
                    HorizontalDivider()
                }
                if (allChats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No hay chats aún", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(allChats, key = { it.id }) { chat ->
                            val isSelected = chat.id in selectedChats
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .then(
                                        @OptIn(ExperimentalFoundationApi::class)
                                        Modifier.combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) selectedChats = if (isSelected) selectedChats - chat.id else selectedChats + chat.id
                                                else { scope.launch { drawerState.close() }; viewModel.loadChat(chat.id) }
                                            },
                                            onLongClick = { if (!isSelectionMode) showDeleteMenu = chat.id }
                                        )
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelectionMode) { Checkbox(checked = isSelected, onCheckedChange = { checked -> selectedChats = if (checked) selectedChats + chat.id else selectedChats - chat.id }, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(4.dp)) }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chat.title.ifEmpty { "Nuevo chat" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(chat.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (chat.isPinned) { Icon(Icons.Outlined.PushPin, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                                }
                            }
                            DropdownMenu(expanded = showDeleteMenu == chat.id, onDismissRequest = { showDeleteMenu = null }) {
                                DropdownMenuItem(text = { Text(if (chat.isPinned) "Desfijar" else "Fijar") }, onClick = { viewModel.pinChat(chat.id, !chat.isPinned); showDeleteMenu = null }, leadingIcon = { Icon(Icons.Outlined.PushPin, null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { viewModel.deleteChat(chat.id); showDeleteMenu = null }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) })
                            }
                        }
                    }
                }
            }
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Surface(shadowElevation = 8.dp) {
                        Column {
                            TopAppBar(
                                scrollBehavior = scrollBehavior,
                                title = { Text("ThunderAI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Outlined.Menu, "Menú") } },
                                actions = {
                                    IconButton(onClick = { viewModel.newChat() }) { Icon(Icons.Outlined.Add, "Nuevo chat") }
                                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                                }
                            )
                            Text(text = if (isOffline) "OFFLINE" else "ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isOffline) Color(0xFFE53935) else Color(0xFF4CAF50), modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).imePadding()) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(listState)) {
                        AnimatedVisibility(visible = isOffline, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(text = context.getString(R.string.no_internet_connection).uppercase(), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }

                        if (messages.isEmpty() && chatId == -1L) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("\u26A1", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PersianText(text = "Hola, ${userName ?: "Usuario"}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PersianText(text = "\u00BFEn qu\u00E9 puedo ayudarte?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        messages.forEachIndexed { index, msg ->
                            val isUser = msg.role == "user"
                            val codeBlocks = extractCodeBlocks(msg.content)
                            var offsetX by remember { mutableFloatStateOf(0f) }

                            Box(
                                modifier = Modifier.fillMaxWidth().pointerInput(msg.content) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = { if (offsetX > 100f) replyTarget = Pair(msg.content, index); offsetX = 0f },
                                        onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount }
                                    )
                                }
                            ) {
                                if (codeBlocks.isNotEmpty() && !isUser) {
                                    MessageWithCodeBlocks(content = msg.content, codeBlocks = codeBlocks, isUser = isUser, fontSize = fontSize, onLongPress = { showBubbleMenu = index }, isTypewriter = chatId == -1L && !isUser && index == messages.size - 1 && isLoading, isNewChat = chatId == -1L, context = context)
                                } else {
                                    ChatBubbleDeepSeek(content = msg.content, isUser = isUser, fontSize = fontSize, onLongPress = { showBubbleMenu = index }, isTypewriter = chatId == -1L && !isUser && index == messages.size - 1 && isLoading, isNewChat = chatId == -1L)
                                }
                            }

                            DropdownMenu(expanded = showBubbleMenu == index, onDismissRequest = { showBubbleMenu = null }) {
                                DropdownMenuItem(text = { Text("Copiar") }, onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("", msg.content))
                                    Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                                    showBubbleMenu = null
                                }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp)) })
                                if (!isUser) {
                                    DropdownMenuItem(text = { Text("Regenerar") }, onClick = { viewModel.regenerate(); showBubbleMenu = null }, leadingIcon = { Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp)) })
                                    DropdownMenuItem(text = { Text("Me gusta") }, onClick = { showLikeFeedback = "Me gusto tu respuesta."; showBubbleMenu = null }, leadingIcon = { Icon(Icons.Outlined.ThumbUp, null, modifier = Modifier.size(18.dp)) })
                                    DropdownMenuItem(text = { Text("No me gusta") }, onClick = { showLikeFeedback = "No me gust\u00F3 tu respuesta."; showBubbleMenu = null }, leadingIcon = { Icon(Icons.Outlined.ThumbDown, null, modifier = Modifier.size(18.dp)) })
                                }
                                DropdownMenuItem(text = { Text("Responder") }, onClick = { replyTarget = Pair(msg.content, index); showBubbleMenu = null }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Compartir") }, onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, msg.content) }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
                                    showBubbleMenu = null
                                }, leadingIcon = { Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp)) })
                            }
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escribiendo...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    AnimatedVisibility(visible = replyTarget != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Outlined.Reply, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Respondiendo a: ${replyTarget?.first?.take(50)}...", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { replyTarget = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Close, "Cancelar", modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(28.dp), shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (inputEnabled) {
                                IconButton(onClick = { viewModel.startVoiceInput(context) { result -> chatInput = result } }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Mic, "Voz", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                OutlinedTextField(
                                    value = chatInput, onValueChange = { chatInput = it }, modifier = Modifier.weight(1f),
                                    placeholder = { PersianText(stringResource(R.string.chat_input), style = MaterialTheme.typography.bodyMedium, fontSize = fontSize.sp) },
                                    maxLines = 4, enabled = !isLoading,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, disabledBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                    keyboardOptions = KeyboardOptions(imeAction = if (chatInput.isNotBlank()) ImeAction.Send else ImeAction.None, keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (chatInput.isNotBlank()) {
                                            scope.launch { viewModel.sendMessage(chatInput.trim(), replyTo = replyTarget?.first); chatInput = ""; replyTarget = null }
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = {
                                    if (chatInput.isNotBlank()) {
                                        scope.launch { viewModel.sendMessage(chatInput.trim(), replyTo = replyTarget?.first); chatInput = ""; replyTarget = null }
                                    }
                                }, enabled = chatInput.isNotBlank() && !isLoading, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Send, stringResource(R.string.send), tint = if (chatInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FloatingActionButton(onClick = { viewModel.cancelRequest() }, containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Stop, stringResource(R.string.cancel), modifier = Modifier.size(20.dp)) } }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleDeepSeek(content: String, isUser: Boolean, fontSize: Int, onLongPress: () -> Unit, isTypewriter: Boolean, isNewChat: Boolean) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = arrangement) {
        Surface(modifier = Modifier.widthIn(max = 320.dp).combinedClickable(onClick = {}, onLongClick = onLongPress), shape = shape, color = bgColor, shadowElevation = 0.dp) {
            if (isTypewriter && isNewChat) TypewriterText(text = content, modifier = Modifier.padding(12.dp), fontSize = fontSize.sp, color = textColor)
            else Text(text = content, modifier = Modifier.padding(12.dp), fontSize = fontSize.sp, color = textColor)
        }
    }
}

data class CodeBlock(val language: String, val code: String)
fun extractCodeBlocks(text: String): List<CodeBlock> {
    val regex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    return regex.findAll(text).map { result ->
        CodeBlock(language = result.groupValues[1].ifEmpty { "code" }, code = result.groupValues[2].trim())
    }.toList()
}

@Composable
fun CodeBlockCard(language: String, code: String, context: Context) {
    var showCopied by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = language, color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("", code)); showCopied = true
                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.size(28.dp)) { Icon(if (showCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp)) }
            }
            HorizontalDivider(color = Color(0xFF2A2A4E))
            Text(text = code, modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFFE0E0E0))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageWithCodeBlocks(content: String, codeBlocks: List<CodeBlock>, isUser: Boolean, fontSize: Int, onLongPress: () -> Unit, isTypewriter: Boolean, isNewChat: Boolean, context: Context) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalAlignment = if (arrangement == Arrangement.End) Alignment.End else Alignment.Start) {
        Surface(modifier = Modifier.widthIn(max = 350.dp).combinedClickable(onClick = {}, onLongClick = onLongPress), shape = shape, color = bgColor, shadowElevation = 0.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                val firstCodeIndex = content.indexOf("```")
                if (firstCodeIndex > 0) {
                    val textBefore = content.substring(0, firstCodeIndex).trim()
                    if (textBefore.isNotEmpty()) { if (isTypewriter && isNewChat) TypewriterText(text = textBefore, fontSize = fontSize.sp, color = textColor) else Text(text = textBefore, fontSize = fontSize.sp, color = textColor); Spacer(modifier = Modifier.height(8.dp)) }
                }
                codeBlocks.forEach { block -> CodeBlockCard(language = block.language, code = block.code, context = context); Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}