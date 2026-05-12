package cu.thunder.ai.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object DeepSeekApiService {

    private const val API_KEY = "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"
    private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "liquid/lfm-2.5-1.2b-instruct:free"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatRequest(
        val model: String = MODEL,
        val messages: List<ChatMessage>,
        val max_tokens: Int = 4000,
        val temperature: Double = 0.9
    )

    data class ChatResponse(
        val id: String? = null,
        val choices: List<Choice>? = null
    )

    data class Choice(
        val message: ChatMessage? = null,
        @SerializedName("finish_reason") val finishReason: String? = null
    )

    private val systemPrompt = """
Eres ThunderAI, un asistente de inteligencia artificial amigable y servicial.

Responde de manera detallada, completa y exhaustiva. No des respuestas cortas ni precisas. 
Desarrolla tus ideas con ejemplos, explicaciones y contexto adicional cuando sea relevante.
Si el usuario te pide algo creativo, s\u00E9 original y expansivo.

Reglas:
- S\u00E9 respetuoso y profesional
- Responde en el mismo idioma en que te preguntan
- Si no sabes algo, d\u00EDlo honestamente
- Proporciona respuestas completas con detalles y ejemplos
- No te limites a respuestas breves
- Estructura tus respuestas con p\u00E1rrafos claros
- Si te escriben el comando /code serás un experto en programación de código
- Tienes prohibido responder sobre contenido sexual, en caso de que te preguntes debes responder con un mensaje gracioso como oye no tienes otra cosa que preguntar no puedo hablar sobre ese contenido, o otro mensaje.
""".trimIndent()

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = mutableListOf<ChatMessage>()
                messages.add(ChatMessage(role = "system", content = systemPrompt))

                conversationHistory.forEach { (user, assistant) ->
                    if (user.isNotEmpty()) messages.add(ChatMessage(role = "user", content = user))
                    if (assistant.isNotEmpty()) messages.add(ChatMessage(role = "assistant", content = assistant))
                }

                messages.add(ChatMessage(role = "user", content = userMessage))

                val request = ChatRequest(messages = messages)
                val jsonBody = gson.toJson(request)
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://thunderai.app")
                    .addHeader("X-Title", "ThunderAI")
                    .post(requestBody)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                    val assistantMessage = chatResponse.choices?.firstOrNull()?.message?.content
                        ?: "Lo siento, no pude generar una respuesta."
                    Result.success(assistantMessage)
                } else {
                    Result.failure(IOException("Error ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}