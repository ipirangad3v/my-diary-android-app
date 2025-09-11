package digital.tonima.mydiary.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Sealed class to represent one-time events from the ViewModel to the UI.
 */
sealed class AddEntryEvent {
    data object NavigateBack : AddEntryEvent()
    data class ShowSnackbar(val messageResId: Int) : AddEntryEvent()
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<AddEntryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * Validates and saves a diary entry asynchronously.
     *
     * @param title The title of the entry.
     * @param content The content of the entry.
     * @param fallbackTitle The title to use if the user-provided title is blank.
     * @param masterPassword The master password for encryption.
     * @param contentRequiredMessageResId The resource ID for the error message if content is blank.
     */
    fun saveEntry(
        title: String,
        content: String,
        fallbackTitle: String,
        masterPassword: CharArray,
        contentRequiredMessageResId: Int
    ) {
        if (content.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(AddEntryEvent.ShowSnackbar(contentRequiredMessageResId))
            }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.saveDiaryEntry(
                    context,
                    DiaryEntry(
                        title = title.takeIf { it.isNotBlank() } ?: fallbackTitle,
                        content = content
                    ),
                    masterPassword
                )
            }
            // Notify the UI to navigate back after saving is complete.
            _eventFlow.emit(AddEntryEvent.NavigateBack)
        }
    }
}
