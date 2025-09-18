data class User(
    val id: String,
    val name: String,
    val profileImage: String = "",
    val isCurrentlyReading: Boolean = false
)
