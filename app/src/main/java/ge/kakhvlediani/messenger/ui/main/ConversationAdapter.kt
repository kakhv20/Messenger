package ge.kakhvlediani.messenger.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.ItemConversationBinding
import ge.kakhvlediani.messenger.model.Conversation
import ge.kakhvlediani.messenger.model.User
import ge.kakhvlediani.messenger.utils.DateUtils

class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private var conversations = listOf<Conversation>()
    private var users = mapOf<String, User>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(conversations: List<Conversation>, users: Map<String, User>) {
        this.conversations = conversations
        this.users = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    inner class ViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (conversation.isGroup) {
                val otherUsers = conversation.participants
                    .filter { it != currentUserId }
                    .mapNotNull { users[it] }

                binding.tvUsername.text = otherUsers.joinToString(", ") { it.nickname }
                val photoUrl = otherUsers.firstOrNull()?.profileImageUrl
                loadProfileImage(photoUrl)
            } else {
                val otherUser = users[conversation.participants.firstOrNull {
                    it != currentUserId
                }]

                binding.tvUsername.text = otherUser?.nickname ?: "Unknown"
                loadProfileImage(otherUser?.profileImageUrl)
            }

            binding.tvLastMessage.text = conversation.lastMessage
            binding.tvTime.text = DateUtils.formatMessageTime(conversation.timestamp)

            binding.root.setOnClickListener { onItemClick(conversation) }
        }

        private fun loadProfileImage(url: String?) {
            Glide.with(binding.root.context)
                .load(url)
                .placeholder(R.drawable.ic_batman_placeholder)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }
}