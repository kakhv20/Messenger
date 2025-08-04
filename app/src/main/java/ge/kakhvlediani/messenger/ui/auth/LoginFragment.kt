package ge.kakhvlediani.messenger.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.FragmentLoginBinding
import ge.kakhvlediani.messenger.utils.LoadingDialog

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireContext())

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val nickname = binding.etNickname.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInput(nickname, password)) {
                viewModel.login(nickname, password)
            }
        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun validateInput(nickname: String, password: String): Boolean {
        return when {
            nickname.isEmpty() -> {
                binding.etNickname.error = "Nickname is required"
                false
            }

            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                false
            }

            else -> true
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            loadingDialog.dismiss()

            result.fold(
                onSuccess = {
                    findNavController().navigate(R.id.action_login_to_main)
                },
                onFailure = { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: "Login failed",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            )
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}