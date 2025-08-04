package ge.kakhvlediani.messenger.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import ge.kakhvlediani.messenger.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<Result<FirebaseUser>>()
    val loginState: LiveData<Result<FirebaseUser>> = _loginState

    private val _registerState = MutableLiveData<Result<FirebaseUser>>()
    val registerState: LiveData<Result<FirebaseUser>> = _registerState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(nickname: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val email = "$nickname@messenger.com"
            _loginState.value = repository.login(email, password)
            _isLoading.value = false
        }
    }

    fun register(nickname: String, password: String, profession: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registerState.value = repository.register(nickname, password, profession)
            _isLoading.value = false
        }
    }
}