package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ManualPasswordUiState(
    val password: String = ""
)

@HiltViewModel
class ManualPasswordViewModel
    @Inject
    constructor() : ViewModel() {

        private val _uiState = MutableStateFlow(ManualPasswordUiState())
        val uiState = _uiState.asStateFlow()

        fun onPasswordChange(password: String) {
            _uiState.update { it.copy(password = password) }
        }
    }
