package ge.kakhvlediani.messenger.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import ge.kakhvlediani.messenger.model.Conversation
import ge.kakhvlediani.messenger.model.Message
import ge.kakhvlediani.messenger.model.User
import ge.kakhvlediani.messenger.repository.MessageRepository
import ge.kakhvlediani.messenger.repository.UserRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val messageRepository = MessageRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _conversation = MutableLiveData<Conversation?>()
    val conversation: LiveData<Conversation?> = _conversation

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _users = MutableLiveData<Map<String, User>>()
    val users: LiveData<Map<String, User>> = _users

    private val _messageSentEvent = MutableLiveData<Boolean>()
    val messageSentEvent: LiveData<Boolean> = _messageSentEvent

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var conversationId: String = ""

    fun setConversationId(id: String) {
        conversationId = id
        loadConversation()
        loadMessages()
    }

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    private fun loadConversation() {
        viewModelScope.launch {
            messageRepository.getConversation(conversationId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect { conv ->
                    _conversation.value = conv
                    conv?.let { loadUsers(it.participants) }
                }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(conversationId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    private fun loadUsers(userIds: List<String>) {
        viewModelScope.launch {
            val usersMap = mutableMapOf<String, User>()

            userIds.forEach { userId ->
                try {
                    userRepository.getUserProfile(userId)
                        .catch { e ->
                        }
                        .collect { user ->
                            user?.let {
                                usersMap[userId] = it
                                _users.value = usersMap.toMap()
                            }
                        }
                } catch (e: Exception) {
                }
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val result = messageRepository.sendMessage(conversationId, text)

            result.fold(
                onSuccess = {
                    _messageSentEvent.value = true

                    if (text == "Make Group" && _conversation.value?.isGroup == false) {
                    }
                },
                onFailure = { e ->
                    _error.value = e.message
                }
            )
        }
    }

    fun addUserToGroup(userId: String) {
        viewModelScope.launch {
            val result = messageRepository.addUserToConversation(conversationId, userId)

            result.fold(
                onSuccess = {
                    loadConversation()
                },
                onFailure = { e ->
                    _error.value = e.message
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}