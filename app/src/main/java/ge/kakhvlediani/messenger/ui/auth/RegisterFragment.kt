package ge.kakhvlediani.messenger.ui.auth

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.FragmentRegisterBinding
import ge.kakhvlediani.messenger.utils.LoadingDialog

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireContext())

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val nickname = binding.etNickname.text.toString()
            val password = binding.etPassword.text.toString()
            val profession = binding.etProfession.text.toString()

            if (validateInput(nickname, password, profession)) {
                viewModel.register(nickname, password, profession)
            }
        }
    }

    private fun validateInput(nickname: String, password: String, profession: String): Boolean {
        return when {
            nickname.isEmpty() -> {
                binding.etNickname.error = "Nickname is required"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                false
            }
            profession.isEmpty() -> {
                binding.etProfession.error = "What I do is required"
                false
            }
            else -> true
        }
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { result ->
            Log.d(TAG, "Register state changed")

            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Registration successful, navigating to main")
                    findNavController().navigate(R.id.action_register_to_main)
                },
                onFailure = { error ->
                    Log.e(TAG, "Registration failed", error)
                    Snackbar.make(
                        binding.root,
                        error.message ?: "Registration failed",
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