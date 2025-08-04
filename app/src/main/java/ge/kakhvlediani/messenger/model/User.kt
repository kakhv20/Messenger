package ge.kakhvlediani.messenger.model

import com.google.firebase.database.PropertyName

data class User(
    @PropertyName("uid")
    val uid: String = "",

    @PropertyName("nickname")
    val nickname: String = "",

    @PropertyName("profession")
    val profession: String = "",

    @PropertyName("profileImageUrl")
    val profileImageUrl: String = ""
)