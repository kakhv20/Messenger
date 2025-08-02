package ge.kakhvlediani.messenger.model

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    val isGroup: Boolean = false
)