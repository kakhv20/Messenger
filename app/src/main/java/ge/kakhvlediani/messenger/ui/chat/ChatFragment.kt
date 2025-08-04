package ge.kakhvlediani.messenger.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ge.kakhvlediani.messenger.databinding.FragmentChatBinding
import ge.kakhvlediani.messenger.model.Conversation
import ge.kakhvlediani.messenger.utils.hideKeyboard

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setConversationId(args.conversationId)

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("skipLoading", true)
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { message ->
            if (message.text == "Make Group") {
                handleMakeGroupCommand()
            }
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupMessageInput() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.etMessage.text.clear()
                hideKeyboard()
            }
        }

        binding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.rvMessages.postDelayed({
                    if (messageAdapter.itemCount > 0) {
                        binding.rvMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
                    }
                }, 100)
            }
        }
    }

    private fun handleMakeGroupCommand() {
        viewModel.conversation.value?.let { conversation ->
            if (!conversation.isGroup) {
                val action = ChatFragmentDirections.actionChatToSearch()
                findNavController().navigate(action)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.conversation.observe(viewLifecycleOwner) { conversation ->
            conversation?.let {
                updateToolbarTitle(it)
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            val usersMap = viewModel.users.value ?: emptyMap()
            messageAdapter.submitList(messages, usersMap)
            if (messages.isNotEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            val messages = viewModel.messages.value ?: emptyList()
            messageAdapter.submitList(messages, users)

            viewModel.conversation.value?.let { updateToolbarTitle(it) }
        }

        viewModel.messageSentEvent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                binding.rvMessages.postDelayed({
                    if (messageAdapter.itemCount > 0) {
                        binding.rvMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
                    }
                }, 100)
            }
        }
    }

    private fun updateToolbarTitle(conversation: Conversation) {
        val currentUserId = viewModel.getCurrentUserId()

        if (conversation.isGroup) {
            val participantNames = conversation.participants
                .filter { it != currentUserId }
                .mapNotNull { userId ->
                    viewModel.users.value?.get(userId)?.nickname
                }
                .joinToString(", ")

            binding.tvTitle.text = if (participantNames.isNotEmpty()) {
                participantNames
            } else {
                "Group Chat"
            }
        } else {
            val otherUserId = conversation.participants.firstOrNull { it != currentUserId }
            val otherUser = if (otherUserId != null) {
                viewModel.users.value?.get(otherUserId)
            } else null
            binding.tvTitle.text = otherUser?.nickname ?: "Chat"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
