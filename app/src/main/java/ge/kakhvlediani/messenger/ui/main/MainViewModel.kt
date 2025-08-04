package ge.kakhvlediani.messenger.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ge.kakhvlediani.messenger.model.Conversation
import ge.kakhvlediani.messenger.model.User
import ge.kakhvlediani.messenger.repository.MessageRepository
import ge.kakhvlediani.messenger.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val messageRepository = MessageRepository()
    private val userRepository = UserRepository()

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _users = MutableLiveData<Map<String, User>>()
    val users: LiveData<Map<String, User>> = _users

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allConversations = listOf<Conversation>()
    private var loadingJob: Job? = null
    private var isDataLoaded = false

    init {
    }

    fun loadConversations(forceRefresh: Boolean = false) {
        if (loadingJob?.isActive == true && !forceRefresh) {
            Log.d(TAG, "Already loading, skipping")
            return
        }

        if (isDataLoaded && !forceRefresh) {
            Log.d(TAG, "Data already loaded, skipping")
            _isLoading.value = false
            return
        }

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            Log.d(TAG, "Starting to load conversations")
            _isLoading.value = true

            try {
                messageRepository.getConversations()
                    .catch { e ->
                        Log.e(TAG, "Error loading conversations", e)
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { conversationList ->
                        Log.d(TAG, "Received ${conversationList.size} conversations")
                        allConversations = conversationList
                        _conversations.value = conversationList
                        isDataLoaded = true

                        if (conversationList.isEmpty()) {
                            Log.d(TAG, "No conversations, stopping loading")
                            _isLoading.value = false
                        } else {
                            val userIds = conversationList.flatMap { it.participants }.distinct()
                            loadUsersWithTimeout(userIds)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadConversations", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadUsersWithTimeout(userIds: List<String>) {
        Log.d(TAG, "Loading ${userIds.size} users")

        try {
            val usersMap = mutableMapOf<String, User>()
            var loadedCount = 0

            userIds.forEach { userId ->
                try {
                    if (_users.value?.containsKey(userId) == true) {
                        loadedCount++
                        return@forEach
                    }

                    userRepository.getUserProfile(userId)
                        .catch { e ->
                            Log.e(TAG, "Error loading user $userId", e)
                            loadedCount++
                        }
                        .collect { user ->
                            user?.let {
                                usersMap[userId] = it
                            }
                            loadedCount++

                            val currentUsers = _users.value?.toMutableMap() ?: mutableMapOf()
                            currentUsers.putAll(usersMap)
                            _users.value = currentUsers

                            if (loadedCount >= userIds.size) {
                                Log.d(TAG, "All users loaded")
                                _isLoading.value = false
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading user $userId", e)
                    loadedCount++
                    if (loadedCount >= userIds.size) {
                        _isLoading.value = false
                    }
                }
            }

            delay(3000)
            if (_isLoading.value == true) {
                Log.d(TAG, "User loading timeout")
                _isLoading.value = false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadUsersWithTimeout", e)
            _isLoading.value = false
        }
    }

    fun filterConversations(query: String) {
        if (query.isEmpty()) {
            _conversations.value = allConversations
        } else {
            _conversations.value = allConversations.filter { conversation ->
                val otherUsers = conversation.participants.mapNotNull { userId ->
                    _users.value?.get(userId)
                }
                otherUsers.any { user ->
                    user.nickname.contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun refresh() {
        loadConversations(forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
    }
}