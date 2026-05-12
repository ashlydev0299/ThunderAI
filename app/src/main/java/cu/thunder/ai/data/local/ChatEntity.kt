package cu.thunder.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "Nuevo chat",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)