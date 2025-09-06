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

            if (!masterPassword.contentEquals(other.masterPassword)) return false

            return true
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

            if (!masterPassword.contentEquals(other.masterPassword)) return false

            return true
        }

        override fun hashCode(): Int {
            return masterPassword.contentHashCode()
        }
    }
}