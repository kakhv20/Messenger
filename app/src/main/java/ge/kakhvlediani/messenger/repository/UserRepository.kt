package ge.kakhvlediani.messenger.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import ge.kakhvlediani.messenger.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val snapshot = database.reference.child("users")
                .orderByChild("nickname")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get().await()

            snapshot.children.mapNotNull { it.getValue(User::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUserProfile(userId: String): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = database.reference.child("users").child(userId)
        ref.addValueEventListener(listener)

        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateProfile(
        nickname: String,
        profession: String,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val updates = mutableMapOf<String, Any>(
                "nickname" to nickname,
                "profession" to profession
            )
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            database.reference.child("users").child(userId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}