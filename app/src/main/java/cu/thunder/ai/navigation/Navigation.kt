package cu.thunder.ai.navigation

object NavRoutes {
    const val CHAT_MAIN = "chat_main"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun chat(chatId: Long = -1L) = "chat/$chatId"
}