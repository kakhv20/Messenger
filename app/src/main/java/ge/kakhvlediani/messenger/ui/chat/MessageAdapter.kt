package ge.kakhvlediani.messenger.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.ItemMessageReceivedBinding
import ge.kakhvlediani.messenger.databinding.ItemMessageSentBinding
import ge.kakhvlediani.messenger.model.Message
import ge.kakhvlediani.messenger.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val onMessageClick: (Message) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    private var messages = listOf<Message>()
    private var users = mapOf<String, User>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun submitList(messages: List<Message>, users: Map<String, User>) {
        this.messages = messages
        this.users = users
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(binding)
            }

            else -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(messages[position])
            is ReceivedMessageViewHolder -> holder.bind(messages[position])
        }
    }

    override fun getItemCount() = messages.size

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)

            if (message.text == "Make Group") {
                binding.tvMessage.setOnClickListener {
                    onMessageClick(message)
                }
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val sender = users[message.senderId]
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)

            Glide.with(binding.root.context)
                .load(sender?.profileImageUrl)
                .placeholder(R.drawable.ic_batman_placeholder)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}