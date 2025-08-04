package ge.kakhvlediani.messenger.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.FragmentProfileBinding
import ge.kakhvlediani.messenger.utils.LoadingDialog

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireContext())

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnUpdate.setOnClickListener {
            val nickname = binding.etNickname.text.toString()
            val profession = binding.etProfession.text.toString()

            if (nickname.isNotEmpty() && profession.isNotEmpty()) {
                viewModel.updateProfile(nickname, profession)
            }
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etNickname.setText(it.nickname)
                binding.etProfession.setText(it.profession)

                Glide.with(this)
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.ic_batman_placeholder)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            loadingDialog.dismiss()

            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, "Profile updated", Snackbar.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: "Update failed",
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