package digital.tonima.mydiary.ui.screens

sealed class AppScreen {
    object Locked : AppScreen()
    object SetupPassword : AppScreen()
    object RecoverPassword : AppScreen()
    data class Main(val masterPassword: CharArray) : AppScreen() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Main

            return masterPassword.contentEquals(other.masterPassword)
        }

        override fun hashCode(): Int {
            return masterPassword.contentHashCode()
        }
    }

    data class AddEntry(val masterPassword: CharArray) : AppScreen() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AddEntry

            return masterPassword.contentEquals(other.masterPassword)
        }

        override fun hashCode(): Int {
            return masterPassword.contentHashCode()
        }
    }
}