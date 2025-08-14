import java.util.*

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    var isDeleted: Boolean = false
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val userId1: String,
    val userId2: String,
    val messages: MutableList<Message> = mutableListOf(),
    var isDeleted: Boolean = false
) {
    fun getLastMessageText(): String =
        messages
            .filterNot { it.isDeleted }
            .lastOrNull()
            ?.text ?: "нет сообщений"


    fun hasParticipants(user1: String, user2: String): Boolean =
         (userId1 == user1 && userId2 == user2) ||
                (userId1 == user2 && userId2 == user1)

}

object ChatStorage {
    private val chats = mutableListOf<Chat>()

    fun findOrCreateChat(user1: String, user2: String): Chat =
         chats.asSequence()
             .firstOrNull { it.hasParticipants(user1, user2) }
            ?: Chat(userId1 = user1, userId2 = user2).also { chats.add(it) }


    fun findChat(user1: String, user2: String): Chat? =
        chats.asSequence()
            .firstOrNull { it.hasParticipants(user1, user2) }


    fun getChatsForUser(userId: String): List<Chat> =
         chats.asSequence()
             .filter {
            !it.isDeleted && (it.userId1 == userId || it.userId2 == userId)
        }
             .toList()


    fun clear() =
        chats.clear()

}
class MessageService(private val currentUserId: String) {

    // Отправка сообщения
    fun sendMessage(recipientId: String, text: String) {
        val chat = ChatStorage.findOrCreateChat(currentUserId, recipientId)
        chat.messages.add(Message(senderId = currentUserId, text = text))
    }

    // Получение списка чатов пользователя
    fun getChats(): List<Chat> =
        ChatStorage.getChatsForUser(currentUserId)


    // Получение последних сообщений из всех чатов
    fun getLastMessages(): List<String> =
        getChats().map { it.getLastMessageText() }


    // Получение сообщений из чата с пометкой прочитанными
    fun getMessages(recipientId: String, count: Int, strict: Boolean = false): List<Message> {
        val chat = ChatStorage.findChat(currentUserId, recipientId)
            ?: if (strict) {
                throw NoSuchElementException("Chat with user $recipientId not found")
            } else {
                return emptyList()
            }

        return chat.messages //optimized
            .asSequence()
            .filterNot { it.isDeleted }
            .toList()
            .takeLast(count)
            .onEach { if (it.senderId != currentUserId) it.isRead = true }
    }

    // Создание сообщения в существующем чате
    fun createMessage(recipientId: String, text: String) {
        ChatStorage.findChat(currentUserId, recipientId)?.let { chat ->
            chat.messages.add(Message(senderId = currentUserId, text = text))
        }
    }

    // Удаление сообщения
    fun deleteMessage(messageId: String) {
        getChats().asSequence()
            .forEach { chat ->
            chat.messages.firstOrNull { it.id == messageId }?.isDeleted = true
        }
    }

    // Удаление чата
    fun deleteChat(recipientId: String) {
        ChatStorage.findChat(currentUserId, recipientId)?.isDeleted = true
    }

    // Получение количества непрочитанных чатов
    fun getUnreadChatsCount(): Int =
        getChats().asSequence()
            .count { chat ->
            chat.messages.any { message ->
                !message.isDeleted &&
                        !message.isRead &&
                        message.senderId != currentUserId
            }
        }

} //можно ли в какие функции добавить .asSequence?

// Пример использования
fun main() {
    val service = MessageService(currentUserId = "user1")

    // Отправка сообщений
    service.sendMessage("user2", "Привет!")
    service.sendMessage("user2", "Как дела?")
    service.sendMessage("user3", "Здравствуйте!")

    // Получение списка чатов
    println("Чаты: ${service.getChats().map { it.id }}")

    // Получение последних сообщений
    println("Последние сообщения: ${service.getLastMessages()}")

    // Получение сообщений из чата с user2 (помечаются прочитанными)
    println("Сообщения с user2: ${service.getMessages("user2", 2).map { it.text }}")

    // Удаление сообщения
    val messageId = service.getMessages("user2", 1).first().id
    service.deleteMessage(messageId)

    // Проверка непрочитанных чатов
    println("Непрочитанных чатов: ${service.getUnreadChatsCount()}")

    // Удаление чата
    service.deleteChat("user3")
    println("Чаты после удаления: ${service.getChats().size}")
}