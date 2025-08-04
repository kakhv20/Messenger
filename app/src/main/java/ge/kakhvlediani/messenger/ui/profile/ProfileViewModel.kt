package ge.kakhvlediani.messenger.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import ge.kakhvlediani.messenger.model.User
import ge.kakhvlediani.messenger.repository.AuthRepository
import ge.kakhvlediani.messenger.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            userRepository.getUserProfile(userId).collect { userData ->
                _user.value = userData
            }
        }
    }

    fun updateProfile(nickname: String, profession: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _updateResult.value = userRepository.updateProfile(nickname, profession)
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.logout()
    }
}