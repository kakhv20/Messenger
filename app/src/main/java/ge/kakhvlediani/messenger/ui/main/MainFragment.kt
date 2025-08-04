package ge.kakhvlediani.messenger.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.FragmentMainBinding
import ge.kakhvlediani.messenger.utils.LoadingDialog

class MainFragment : Fragment() {

    companion object {
        private const val TAG = "MainFragment"
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var conversationAdapter: ConversationAdapter
    private var loadingDialog: LoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "MainFragment onViewCreated")

        val skipLoading = findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.get<Boolean>("skipLoading") ?: false

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.remove<Boolean>("skipLoading")

        loadingDialog = LoadingDialog(requireContext())

        setupRecyclerView()
        setupSearch()
        setupBottomNavigation()
        setupFab()
        observeViewModel()

        if (!skipLoading && viewModel.conversations.value == null) {
            Log.d(TAG, "Loading conversations")
            viewModel.loadConversations()
        } else {
            Log.d(TAG, "Skipping load - data already exists or skipLoading flag set")
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            try {
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("skipLoading", true)

                val action = MainFragmentDirections.actionMainToChat(conversation.conversationId)
                findNavController().navigate(action)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error", e)
                Snackbar.make(binding.root, "Navigation error", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.filterConversations(text.toString())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_home

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (binding.rvConversations.adapter?.itemCount == 0) {
                        viewModel.refresh()
                    }
                    true
                }

                R.id.nav_profile -> {
                    try {
                        findNavController().navigate(R.id.action_main_to_profile)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Navigation to profile failed", e)
                        false
                    }
                }

                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabNewChat.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_main_to_search)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to search failed", e)
                Snackbar.make(binding.root, "Navigation error", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            Log.d(TAG, "Conversations updated: ${conversations.size} items")
            conversationAdapter.submitList(conversations, viewModel.users.value ?: emptyMap())

            updateEmptyState(conversations.isEmpty() && viewModel.isLoading.value == false)
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "Users updated: ${users.size} users")
            conversationAdapter.submitList(
                viewModel.conversations.value ?: emptyList(),
                users
            )
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")

            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
                hideLoading()

                Snackbar.make(
                    binding.root,
                    it,
                    Snackbar.LENGTH_LONG
                ).setAction("Retry") {
                    viewModel.refresh()
                }.show()
            }
        }
    }

    private fun showLoading() {
        loadingDialog?.show()

        binding.root.postDelayed({
            if (loadingDialog?.isShowing == true) {
                Log.w(TAG, "Force dismissing loading dialog after timeout")
                hideLoading()
            }
        }, 1000)
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            Log.d(TAG, "No conversations to display")
        } else {
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isLoading.value == false) {
            hideLoading()
        }
    }

    override fun onPause() {
        super.onPause()
        hideLoading()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }
}