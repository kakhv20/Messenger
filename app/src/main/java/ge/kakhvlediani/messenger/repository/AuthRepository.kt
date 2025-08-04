package ge.kakhvlediani.messenger.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import ge.kakhvlediani.messenger.model.User
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        nickname: String,
        password: String,
        profession: String
    ): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Starting registration for nickname: $nickname")

            cleanupFailedRegistrations()

            val email = "$nickname@messenger.com"
            Log.d(TAG, "Creating user with email: $email")

            val authResult = try {
                auth.createUserWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                Log.e(TAG, "Auth creation failed: ${e.message}")
                if (e.message?.contains("already in use") == true) {
                    return Result.failure(Exception("This nickname is already taken"))
                }
                return Result.failure(e)
            }

            Log.d(TAG, "Auth successful, saving to database...")

            val user = User(
                uid = authResult.user!!.uid,
                nickname = nickname,
                profession = profession
            )

            withTimeout(10000) {
                val userRef = database.reference.child("users").child(user.uid)
                userRef.setValue(user).await()

                val verifySnapshot = userRef.get().await()
                if (!verifySnapshot.exists()) {
                    throw Exception("Failed to save user data")
                }
            }

            Log.d(TAG, "Registration completed successfully")
            Result.success(authResult.user!!)

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Registration timeout")
            auth.currentUser?.delete()
            Result.failure(Exception("Registration timed out. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            auth.currentUser?.delete()
            Result.failure(e)
        }
    }

    private suspend fun cleanupFailedRegistrations() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userSnapshot =
                    database.reference.child("users").child(currentUser.uid).get().await()
                if (!userSnapshot.exists()) {
                    Log.d(TAG, "Cleaning up orphaned auth user: ${currentUser.uid}")
                    currentUser.delete().await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed: ${e.message}")
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser
}