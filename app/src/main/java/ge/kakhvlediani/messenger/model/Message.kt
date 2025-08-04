package ge.kakhvlediani.messenger.model

import com.google.firebase.database.PropertyName

data class Message(
    @PropertyName("messageId")
    val messageId: String = "",

    @PropertyName("senderId")
    val senderId: String = "",

    @PropertyName("text")
    val text: String = "",

    @PropertyName("timestamp")
    val timestamp: Long = 0L
)