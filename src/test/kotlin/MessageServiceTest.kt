import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class MessageServiceTest {
    private lateinit var service: MessageService
    private val currentUserId = "user1"
    private val recipient1 = "user2"
    private val recipient2 = "user3"

    @Before
    fun setUp() {
        ChatStorage.clear()
        service = MessageService(currentUserId)
    }

    @Test
    fun `sendMessage should create new chat if not exists`() {
        service.sendMessage(recipient1, "Hello")
        assertEquals(1, service.getChats().size)
    }

    @Test
    fun `sendMessage should add message to existing chat`() {
        service.sendMessage(recipient1, "First")
        service.sendMessage(recipient1, "Second")
        assertEquals(2, service.getMessages(recipient1, 2).size)
    }

    @Test
    fun `getChats should return only non-deleted chats`() {
        service.sendMessage(recipient1, "Hi")
        service.sendMessage(recipient2, "Hey")
        service.deleteChat(recipient1)
        assertEquals(1, service.getChats().size)
    }

    @Test
    fun `getLastMessages should return correct texts`() {
        service.sendMessage(recipient1, "First")
        service.sendMessage(recipient1, "Last")
        assertEquals(listOf("Last"), service.getLastMessages())
    }

    @Test
    fun `getLastMessages should return default text when all messages deleted`() {
        service.sendMessage(recipient1, "To be deleted")
        val messageId = service.getMessages(recipient1, 1)[0].id
        service.deleteMessage(messageId)
        assertEquals(listOf("нет сообщений"), service.getLastMessages())
    }

    @Test
    fun `getMessages should mark incoming messages as read`() {
        // Create chat from other user's perspective
        MessageService(recipient1).sendMessage(currentUserId, "Hello")

        val messages = service.getMessages(recipient1, 1)
        assertTrue(messages[0].isRead)
    }

    @Test
    fun `getMessages should not mark outgoing messages as read`() {
        service.sendMessage(recipient1, "Outgoing")
        val messages = service.getMessages(recipient1, 1)
        assertFalse(messages[0].isRead)
    }

    @Test(expected = NoSuchElementException::class)
    fun `getMessages should throw when chat not found in strict mode`() {
        service.getMessages("non-existent", 1, strict = true)
    }

    @Test
    fun `getMessages should return empty list when chat not found`() {
        assertTrue(service.getMessages("non-existent", 1).isEmpty())
    }

    @Test
    fun `createMessage should add message to existing chat`() {
        service.sendMessage(recipient1, "First")
        service.createMessage(recipient1, "Second")
        assertEquals(2, service.getMessages(recipient1, 2).size)
    }

    @Test
    fun `createMessage should do nothing if chat not exists`() {
        service.createMessage(recipient1, "Test")
        assertTrue(service.getChats().isEmpty())
    }

    @Test
    fun `deleteMessage should mark message as deleted`() {
        service.sendMessage(recipient1, "To delete")
        val messageId = service.getMessages(recipient1, 1)[0].id
        service.deleteMessage(messageId)
        assertTrue(service.getMessages(recipient1, 1).isEmpty())
    }

    @Test
    fun `deleteChat should mark chat as deleted`() {
        service.sendMessage(recipient1, "Hi")
        service.deleteChat(recipient1)
        assertEquals(0, service.getChats().size)
    }

    @Test
    fun `getUnreadChatsCount should count only chats with unread incoming messages`() {
        // Incoming message (should count)
        MessageService(recipient1).sendMessage(currentUserId, "Hi!")

        // Outgoing message (shouldn't count)
        service.sendMessage(recipient2, "Hello")

        assertEquals(1, service.getUnreadChatsCount())
    }

    @Test
    fun `getUnreadChatsCount should return zero after reading messages`() {
        MessageService(recipient1).sendMessage(currentUserId, "Hi")
        assertEquals(1, service.getUnreadChatsCount())

        service.getMessages(recipient1, 1)
        assertEquals(0, service.getUnreadChatsCount())
    }

    @Test
    fun `getUnreadChatsCount should ignore deleted chats`() {
        MessageService(recipient1).sendMessage(currentUserId, "Hi")
        service.deleteChat(recipient1)
        assertEquals(0, service.getUnreadChatsCount())
    }
}