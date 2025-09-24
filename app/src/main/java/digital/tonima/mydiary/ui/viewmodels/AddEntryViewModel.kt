package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.database.repositories.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddEntryUiState(
    val isLoading: Boolean = false,
    val initialTitle: String = "",
    val initialContentHtml: String = "",
    val showDeleteConfirmation: Boolean = false
)

sealed class AddEntryEvent {
    data object NavigateBack : AddEntryEvent()
    data class ShowSnackbar(val messageResId: Int) : AddEntryEvent()
}

@HiltViewModel
class AddEntryViewModel
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(AddEntryUiState())
        val uiState = _uiState.asStateFlow()

        private val _eventFlow = MutableSharedFlow<AddEntryEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        private var currentEntryId: Long? = null

        fun initialize(entryId: Long?, masterPassword: CharArray) {
            if (currentEntryId == entryId) return

            currentEntryId = entryId
            if (entryId != null) {
                loadEntryForEditing(entryId, masterPassword)
            } else {
                _uiState.update { AddEntryUiState() }
            }
        }

        private fun loadEntryForEditing(entryId: Long, masterPassword: CharArray) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val entryPair = withContext(Dispatchers.IO) {
                    diaryRepository.getEntryById(entryId, masterPassword)
                }
                if (entryPair != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialTitle = entryPair.second.title,
                            initialContentHtml = entryPair.second.contentHtml
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        fun saveEntry(
            title: String,
            contentHtml: String,
            fallbackTitle: String,
            masterPassword: CharArray,
            contentRequiredMessageResId: Int
        ) {
            if (contentHtml.isBlank() || contentHtml == "<p><br></p>") {
                viewModelScope.launch {
                    _eventFlow.emit(AddEntryEvent.ShowSnackbar(contentRequiredMessageResId))
                }
                return
            }

            viewModelScope.launch(Dispatchers.IO) {
                val entry = DiaryEntry(
                    title = title.takeIf { it.isNotBlank() } ?: fallbackTitle,
                    contentHtml = contentHtml
                )
                diaryRepository.addOrUpdateEntry(entry, masterPassword, currentEntryId)
                _eventFlow.emit(AddEntryEvent.NavigateBack)
            }
        }

        fun onDeleteRequest() {
            _uiState.update { it.copy(showDeleteConfirmation = true) }
        }

        fun onDismissDeleteDialog() {
            _uiState.update { it.copy(showDeleteConfirmation = false) }
        }

        fun deleteEntry(masterPassword: CharArray) {
            val entryIdToDelete = currentEntryId ?: return
            viewModelScope.launch(Dispatchers.IO) {
                diaryRepository.deleteEntry(entryIdToDelete, masterPassword)
                _eventFlow.emit(AddEntryEvent.NavigateBack)
            }
        }
    }
