package ge.kakhvlediani.messenger.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import ge.kakhvlediani.messenger.model.User
import ge.kakhvlediani.messenger.repository.MessageRepository
import ge.kakhvlediani.messenger.repository.UserRepository
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val messageRepository = MessageRepository()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigationEvent = MutableLiveData<String?>()
    val navigationEvent: LiveData<String?> = _navigationEvent

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val allUsers = userRepository.searchUsers("")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            _users.value = allUsers.filter { it.uid != currentUserId }
            _isLoading.value = false
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val results = userRepository.searchUsers(query)
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            _users.value = results.filter { it.uid != currentUserId }
            _isLoading.value = false
        }
    }

    fun createOrGetConversation(otherUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existingConversation = messageRepository.findExistingConversation(otherUserId)
                val conversationId = existingConversation?.conversationId
                    ?: messageRepository.createConversation(otherUserId)

                _navigationEvent.value = conversationId
            } catch (e: Exception) {
            }
            _isLoading.value = false
        }
    }
}