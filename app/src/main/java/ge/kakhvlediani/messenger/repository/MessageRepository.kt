package ge.kakhvlediani.messenger.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import ge.kakhvlediani.messenger.model.Conversation
import ge.kakhvlediani.messenger.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MessageRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                val conversations = snapshot.children.mapNotNull {
                    it.getValue(Conversation::class.java)?.takeIf { conv ->
                        conv.participants.contains(userId)
                    }
                }
                trySend(conversations.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = database.reference.child("conversations")
        ref.addValueEventListener(listener)

        awaitClose { ref.removeEventListener(listener) }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val messages = mutableListOf<Message>()

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Message::class.java)?.let { message ->
                    messages.add(message)
                    trySend(messages.sortedBy { it.timestamp })
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = database.reference.child("messages").child(conversationId)
        ref.addChildEventListener(listener)

        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val message = Message(
                messageId = messageId,
                senderId = userId,
                text = text,
                timestamp = timestamp
            )

            database.reference.child("messages")
                .child(conversationId)
                .child(messageId)
                .setValue(message)
                .await()

            database.reference.child("conversations")
                .child(conversationId)
                .updateChildren(
                    mapOf(
                        "lastMessage" to text,
                        "timestamp" to timestamp
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConversation(otherUserId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val conversationId = UUID.randomUUID().toString()

        val conversation = Conversation(
            conversationId = conversationId,
            participants = listOf(currentUserId, otherUserId),
            lastMessage = "",
            timestamp = System.currentTimeMillis(),
            isGroup = false
        )

        database.reference.child("conversations")
            .child(conversationId)
            .setValue(conversation)
            .await()

        return conversationId
    }

    suspend fun findExistingConversation(otherUserId: String): Conversation? {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return null

            val snapshot = database.reference.child("conversations").get().await()

            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Conversation::class.java)
            }.firstOrNull { conversation ->
                conversation.participants.size == 2 &&
                        conversation.participants.contains(currentUserId) &&
                        conversation.participants.contains(otherUserId) &&
                        !conversation.isGroup
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversation = snapshot.getValue(Conversation::class.java)
                trySend(conversation)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = database.reference.child("conversations").child(conversationId)
        ref.addValueEventListener(listener)

        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addUserToConversation(conversationId: String, userId: String): Result<Unit> {
        return try {
            val conversationRef = database.reference.child("conversations").child(conversationId)
            val snapshot = conversationRef.get().await()
            val conversation = snapshot.getValue(Conversation::class.java)
                ?: throw Exception("Conversation not found")

            if (conversation.participants.contains(userId)) {
                return Result.success(Unit)
            }

            val updatedParticipants = conversation.participants + userId
            val updates = mapOf(
                "participants" to updatedParticipants,
                "isGroup" to true
            )

            conversationRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}