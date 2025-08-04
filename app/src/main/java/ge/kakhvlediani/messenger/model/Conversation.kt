package ge.kakhvlediani.messenger.model

import com.google.firebase.database.PropertyName

data class Conversation(
    @PropertyName("conversationId")
    val conversationId: String = "",

    @PropertyName("participants")
    val participants: List<String> = emptyList(),

    @PropertyName("lastMessage")
    val lastMessage: String = "",

    @PropertyName("timestamp")
    val timestamp: Long = 0L,

    @PropertyName("isGroup")
    val isGroup: Boolean = false
)