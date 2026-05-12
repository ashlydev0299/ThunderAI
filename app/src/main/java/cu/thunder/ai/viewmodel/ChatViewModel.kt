package cu.thunder.ai.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.thunder.ai.data.local.ChatDao
import cu.thunder.ai.data.local.ChatEntity
import cu.thunder.ai.data.local.ChatMessageEntity
import cu.thunder.ai.data.local.AppDatabase
import cu.thunder.ai.network.DeepSeekApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private lateinit var chatDao: ChatDao
    private var isDbInitialized = false

    fun initDatabase(context: Context) {
        if (!isDbInitialized) {
            chatDao = AppDatabase.getDatabase(context).chatDao()
            isDbInitialized = true
            loadAllChats()
        }
    }

    private val _allChats = MutableStateFlow<List<ChatEntity>>(emptyList())
    val allChats: StateFlow<List<ChatEntity>> = _allChats

    private val _currentChatId = mutableStateOf(-1L)
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages

    private val _currentChatTitle = MutableStateFlow("")
    val currentChatTitle: StateFlow<String> = _currentChatTitle

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _inputEnabled = MutableStateFlow(true)
    val inputEnabled: StateFlow<Boolean> = _inputEnabled

    val snackbarHost = SnackbarHostState()

    private var chatJob: Job? = null

    data class ChatMessage(
        val content: String,
        val role: String
    )

    private var lastTryTime = 0L
    private var tryCount = 0
    private val rateLimitTime = 3600
    private val rateLimitTries = 10

    fun loadAllChats() {
        if (!isDbInitialized) return
        viewModelScope.launch {
            chatDao.getAllChats().collect { chats ->
                _allChats.value = chats.sortedByDescending { it.timestamp }
            }
        }
    }

    fun loadChat(chatId: Long) {
        if (!isDbInitialized) return
        _currentChatId.value = chatId
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId)
            _currentChatTitle.value = chat?.title ?: ""

            chatDao.getMessagesByChatId(chatId).collect { messages ->
                _currentMessages.value = messages.map {
                    ChatMessage(content = it.content, role = it.role)
                }
            }
        }
    }

    fun newChat() {
        _currentChatId.value = -1L
        _currentMessages.value = emptyList()
        _currentChatTitle.value = ""
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (!isDbInitialized) return

        viewModelScope.launch {
            if (isLocallyRateLimited()) {
                snackbarHost.showSnackbar("Has alcanzado el límite. Inténtalo en una hora.")
                return@launch
            }

            val userMsg = ChatMessage(content = content.trim(), role = "user")
            _currentMessages.value = _currentMessages.value + userMsg

            var chatId = _currentChatId.value
            if (chatId == -1L) {
                val title = if (content.length > 30) content.take(30) + "..." else content
                chatId = chatDao.insertChat(
                    ChatEntity(title = title, lastMessage = content, timestamp = System.currentTimeMillis())
                )
                _currentChatId.value = chatId
                _currentChatTitle.value = title
                loadAllChats()
            }

            chatDao.insertMessage(
                ChatMessageEntity(chatId = chatId, role = "user", content = content.trim())
            )
            chatDao.updateChatLastMessage(chatId, content.trim())

            _isLoading.value = true
            _inputEnabled.value = false

            val history = buildHistory()
            chatJob = launch(Dispatchers.IO) {
                val result = DeepSeekApiService.sendMessage(content.trim(), history)

                withContext(Dispatchers.Main) {
                    result.onSuccess { response ->
                        val assistantMsg = ChatMessage(content = response, role = "assistant")
                        _currentMessages.value = _currentMessages.value + assistantMsg

                        launch {
                            chatDao.insertMessage(
                                ChatMessageEntity(chatId = chatId, role = "assistant", content = response)
                            )
                            chatDao.updateChatLastMessage(chatId, response)
                            loadAllChats()

                            if (_currentMessages.value.size > 2) {
                                predictAndUpdateTitle(chatId)
                            }
                        }
                    }.onFailure { error ->
                        val errorMsg = ChatMessage(
                            content = "Error: ${error.message ?: "Falló la conexión"}",
                            role = "assistant"
                        )
                        _currentMessages.value = _currentMessages.value + errorMsg
                    }

                    _isLoading.value = false
                    _inputEnabled.value = true
                }
            }
        }
    }

    fun cancelRequest() {
        chatJob?.cancel()
        _isLoading.value = false
        _inputEnabled.value = true
    }

    fun renameChat(chatId: Long, newTitle: String) {
        if (!isDbInitialized) return
        viewModelScope.launch {
            chatDao.updateChatTitle(chatId, newTitle)
            loadAllChats()
        }
    }

    fun deleteChat(chatId: Long) {
        if (!isDbInitialized) return
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId)
            if (chat != null) {
                chatDao.deleteChat(chat)
                loadAllChats()
                if (_currentChatId.value == chatId) {
                    newChat()
                }
            }
        }
    }

    fun checkConnectivity(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        _isOffline.value = capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun isLocallyRateLimited(): Boolean {
        val now = System.currentTimeMillis() / 1000
        return if (tryCount > 0 && now - lastTryTime < rateLimitTime) {
            if (tryCount < rateLimitTries) {
                tryCount++
                lastTryTime = now
                false
            } else true
        } else {
            tryCount = 1
            lastTryTime = now
            false
        }
    }

    private fun buildHistory(): List<Pair<String, String>> {
        val history = mutableListOf<Pair<String, String>>()
        val msgs = _currentMessages.value
        var i = 0
        while (i < msgs.size - 1) {
            if (msgs[i].role == "user" && msgs[i + 1].role == "assistant") {
                history.add(Pair(msgs[i].content, msgs[i + 1].content))
                i += 2
            } else i++
        }
        return history
    }

    private suspend fun predictAndUpdateTitle(chatId: Long) {
        try {
            val msgs = _currentMessages.value
            val prompt = "find a title for this conversation:\n" + msgs.joinToString("\n") { it.content }
            val result = DeepSeekApiService.sendMessage(prompt)
            result.onSuccess { predictedTitle ->
                val clean = predictedTitle.trim().replace("\\n", "").removeSurrounding("\"")
                if (clean.isNotBlank() && clean.length > 3) {
                    _currentChatTitle.value = clean
                    chatDao.updateChatTitle(chatId, clean)
                    loadAllChats()
                }
            }
        } catch (_: Exception) { }
    }
}