package digital.tonima.mydiary.ui.screens

import digital.tonima.mydiary.ui.screens.BottomBarScreen.Diary

/**
 * Enum to represent the main screens accessible via the bottom navigation bar.
 */
enum class BottomBarScreen {
    Diary, Vault
}

/**
 * Sealed class representing all possible screens/states of the application.
 * This is the single source of truth for navigation logic in MainActivity.
 */
sealed class AppScreen {
    object Locked : AppScreen()
    object SetupPassword : AppScreen()
    object RecoverPassword : AppScreen()

    /**
     * Represents the main authenticated state of the app.
     * @param masterPassword The decrypted master password, required for all crypto operations.
     * @param currentScreen The currently selected screen on the bottom navigation bar.
     */
    data class Principal(
        val masterPassword: CharArray,
        val currentScreen: BottomBarScreen = Diary
    ) : AppScreen() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Principal
            if (!masterPassword.contentEquals(other.masterPassword)) return false
            if (currentScreen != other.currentScreen) return false
            return true
        }

        override fun hashCode(): Int {
            var result = masterPassword.contentHashCode()
            result = 31 * result + currentScreen.hashCode()
            return result
        }
    }

    /**
     * Represents the screen for adding a new diary entry.
     * @param masterPassword The master password, needed to save the new entry.
     */
    data class AddEntry(val masterPassword: CharArray, val fileNameToEdit: String? = null) : AppScreen() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AddEntry

            if (!masterPassword.contentEquals(other.masterPassword)) return false
            if (fileNameToEdit != other.fileNameToEdit) return false

            return true
        }

        override fun hashCode(): Int {
            var result = masterPassword.contentHashCode()
            result = 31 * result + (fileNameToEdit?.hashCode() ?: 0)
            return result
        }
    }
}
