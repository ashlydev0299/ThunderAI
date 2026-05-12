package cu.thunder.ai.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cu.thunder.ai.R
import cu.thunder.ai.data.local.ChatEntity
import cu.thunder.ai.ui.components.PersianText
import cu.thunder.ai.ui.components.PersianTextField
import cu.thunder.ai.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onChatSelected: (Long) -> Unit
) {
    val chats by viewModel.allChats.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = { PersianText(stringResource(R.string.chat_history), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCAC", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    PersianText("No hay chats aún")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chats) { chat ->
                    HistoryItem(
                        chat = chat,
                        onClick = { onChatSelected(chat.id) },
                        onEdit = { newTitle ->
                            viewModel.renameChat(chat.id, newTitle)
                        },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteChat(chat.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    chat: ChatEntity,
    onClick: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    ElevatedCard(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                isMenuExpanded = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersianText(
                    text = stringResource(R.string.title),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                PersianText(
                    text = chat.title.ifEmpty { "Nuevo chat" },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersianText(
                    text = stringResource(R.string.date),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                PersianText(
                    text = SimpleDateFormat("yyyy/MM/dd_HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { PersianText(stringResource(R.string.edit)) },
                    onClick = { isMenuExpanded = false; isEditing = true },
                    leadingIcon = { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
                )
                DropdownMenuItem(
                    text = { PersianText(stringResource(R.string.delete)) },
                    onClick = { isDeleting = true; isMenuExpanded = false },
                    leadingIcon = { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
                )
            }
        }
    }

    if (isDeleting) {
        AlertDialog(
            onDismissRequest = { isDeleting = false },
            title = { PersianText(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                Button(onClick = { isDeleting = false; onDelete() }) {
                    PersianText(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleting = false }) {
                    PersianText(stringResource(R.string.no))
                }
            }
        )
    }

    if (isEditing) {
        var newTitle by remember { mutableStateOf(chat.title) }
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { PersianText(stringResource(R.string.edit)) },
            text = {
                PersianTextField(
                    singleLine = true,
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { PersianText(stringResource(R.string.history_title)) }
                )
            },
            confirmButton = {
                Button(onClick = { isEditing = false; onEdit(newTitle) }) {
                    PersianText(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    PersianText(stringResource(R.string.cancel))
                }
            }
        )
    }
}