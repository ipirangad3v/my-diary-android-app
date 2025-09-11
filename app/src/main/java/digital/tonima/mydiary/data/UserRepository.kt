package digital.tonima.mydiary.data

interface UserRepository {
    fun isProUser(): Boolean
    fun setProUser(isPro: Boolean)
}
