package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.database.entities.DiaryEntryEntity
import digital.tonima.mydiary.database.repositories.DiaryRepository
import digital.tonima.mydiary.delegates.ProUserProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PrincipalScreenUiState(
    val isLoading: Boolean = true,
    val allDecryptedEntries: Map<DiaryEntryEntity, DiaryEntry> = emptyMap(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val selectedEntry: Pair<DiaryEntryEntity, DiaryEntry>? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteAllConfirmation: Boolean = false,
    val showResetAppConfirmation: Boolean = false,
    val showUpgradeConfirmation: Boolean = false
)

@HiltViewModel
class PrincipalViewModel
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        proUserProvider: ProUserProvider
    ) : ViewModel(), ProUserProvider by proUserProvider {

        private val _uiState = MutableStateFlow(PrincipalScreenUiState())
        val uiState = _uiState.asStateFlow()

        fun loadEntries(masterPassword: CharArray) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                diaryRepository.getEntries(masterPassword).collectLatest { decryptedMap ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allDecryptedEntries = decryptedMap
                        )
                    }
                }
            }
        }

        fun deleteAllEntries(masterPassword: CharArray) {
            viewModelScope.launch(Dispatchers.IO) {
                diaryRepository.deleteAllEntries(masterPassword)
            }
            _uiState.update { it.copy(showDeleteAllConfirmation = false) }
        }

        fun onDateSelected(date: LocalDate?) {
            _uiState.update { it.copy(selectedDate = date) }
        }

        fun onDeleteAllRequest() {
            _uiState.update { it.copy(showDeleteAllConfirmation = true) }
        }

        fun onDismissDeleteAllDialog() {
            _uiState.update { it.copy(showDeleteAllConfirmation = false) }
        }

        fun onResetAppRequest() {
            _uiState.update { it.copy(showResetAppConfirmation = true) }
        }

        fun onDismissResetAppDialog() {
            _uiState.update { it.copy(showResetAppConfirmation = false) }
        }

        fun onUpgradeToProRequest() {
            _uiState.update { it.copy(showUpgradeConfirmation = true) }
        }

        fun onPurchaseFlowHandled() {
            _uiState.update { it.copy(showUpgradeConfirmation = false) }
        }
    }
