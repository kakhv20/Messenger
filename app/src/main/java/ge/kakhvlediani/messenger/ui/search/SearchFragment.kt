package ge.kakhvlediani.messenger.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ge.kakhvlediani.messenger.databinding.FragmentSearchBinding
import ge.kakhvlediani.messenger.utils.LoadingDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireContext())

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        viewModel.loadAllUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user ->
            viewModel.createOrGetConversation(user.uid)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            searchJob?.cancel()
            if (text.toString().length >= 3) {
                searchJob = lifecycleScope.launch {
                    delay(500)
                    viewModel.searchUsers(text.toString())
                }
            } else if (text.toString().isEmpty()) {
                viewModel.loadAllUsers()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
            binding.tvNoResults.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { conversationId ->
            conversationId?.let {
                val action = SearchFragmentDirections.actionSearchToChat(it)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}