package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordStrength {
    EMPTY, WEAK, MEDIUM, STRONG
}

data class PasswordSetupUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val errorResId: Int? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.EMPTY,
)

sealed class PasswordSetupEvent {
    data class PasswordSet(val password: CharArray) : PasswordSetupEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PasswordSet

            if (!password.contentEquals(other.password)) return false

            return true
        }

        override fun hashCode(): Int {
            return password.contentHashCode()
        }
    }
}

@HiltViewModel
class PasswordSetupViewModel
    @Inject
    constructor() : ViewModel() {

        private val _uiState = MutableStateFlow(PasswordSetupUiState())
        val uiState = _uiState.asStateFlow()

        private val _eventFlow = MutableSharedFlow<PasswordSetupEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        fun onPasswordChange(password: String) {
            val strength = calculatePasswordStrength(password)
            _uiState.update {
                it.copy(
                    password = password,
                    errorResId = null,
                    passwordStrength = strength,
                )
            }
        }

        private fun calculatePasswordStrength(password: String): PasswordStrength {
            if (password.isEmpty()) return PasswordStrength.EMPTY

            val hasLowercase = password.any { it.isLowerCase() }
            val hasUppercase = password.any { it.isUpperCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { !it.isLetterOrDigit() }

            val criteriaCount = listOf(hasLowercase, hasUppercase, hasDigit, hasSpecialChar).count { it }

            return when {
                password.length < 8 -> PasswordStrength.WEAK
                criteriaCount >= 3 -> PasswordStrength.STRONG
                criteriaCount >= 2 -> PasswordStrength.MEDIUM
                else -> PasswordStrength.WEAK
            }
        }

        fun onConfirmPasswordChange(confirmPassword: String) {
            _uiState.update { it.copy(confirmPassword = confirmPassword, errorResId = null) }
        }

        fun onSavePasswordClicked() {
            val state = _uiState.value
            if (state.password.length < 6) {
                _uiState.update { it.copy(errorResId = R.string.password_too_short) }
                return
            }

            if (state.password != state.confirmPassword) {
                _uiState.update { it.copy(errorResId = R.string.passwords_do_not_match) }
                return
            }

            viewModelScope.launch {
                _eventFlow.emit(PasswordSetupEvent.PasswordSet(state.password.toCharArray()))
            }
        }
    }
