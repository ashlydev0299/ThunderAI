package cu.thunder.ai.navigation

object NavRoutes {
    const val HOME = "home"
    const val CHAT = "chat/{chatId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun chat(chatId: Long = -1L) = "chat/$chatId"
}