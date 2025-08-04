package ge.kakhvlediani.messenger.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ge.kakhvlediani.messenger.R
import ge.kakhvlediani.messenger.databinding.ItemUserBinding
import ge.kakhvlediani.messenger.model.User

class UserAdapter(
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private var users = listOf<User>()

    fun submitList(users: List<User>) {
        this.users = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class ViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUsername.text = user.nickname
            binding.tvProfession.text = user.profession

            Glide.with(binding.root.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_batman_placeholder)
                .circleCrop()
                .into(binding.ivProfile)

            binding.root.setOnClickListener { onItemClick(user) }
        }
    }
}