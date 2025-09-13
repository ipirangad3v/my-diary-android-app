package digital.tonima.mydiary.ui.viewmodels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.delegates.ProUserProvider
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import digital.tonima.mydiary.ui.screens.BottomBarScreen
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Diary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class PrincipalScreenUiState(
    val isLoading: Boolean = true,
    val allDecryptedEntries: Map<File, DiaryEntry> = emptyMap(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val selectedEntry: Pair<File, DiaryEntry>? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteAllConfirmation: Boolean = false,
    val showResetAppConfirmation: Boolean = false,
    val showUpgradeConfirmation: Boolean = false,
    val currentScreen: BottomBarScreen = Diary
)

@HiltViewModel
class PrincipalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    proUserProvider: ProUserProvider
) : ViewModel(), ProUserProvider by proUserProvider {
    private val _uiState = MutableStateFlow(PrincipalScreenUiState())
    val uiState = _uiState.asStateFlow()


    fun onUpgradeToProRequest() {
        _uiState.update { it.copy(showUpgradeConfirmation = true) }
    }

    fun onDismissUpgradeDialog() {
        _uiState.update { it.copy(showUpgradeConfirmation = false) }
    }

    fun loadEntries(masterPassword: CharArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val decrypted = withContext(Dispatchers.IO) {
                val files = PasswordBasedCryptoManager.getAllEntryFiles(context)
                files.mapNotNull { file ->
                    PasswordBasedCryptoManager.readDiaryEntry(context, file, masterPassword)
                        ?.let { entry ->
                            file to entry
                        }
                }.toMap()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    allDecryptedEntries = decrypted
                )
            }
        }
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
        _uiState.update { it.copy(showResetAppConfirmation = false, showDeleteAllConfirmation = false) }
    }

    fun onDateSelected(date: LocalDate?) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun deleteAllEntries(masterPassword: CharArray) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.deleteAllEntries(context)
            }
            _uiState.update { it.copy(showDeleteAllConfirmation = false, showResetAppConfirmation = false) }
            loadEntries(masterPassword)
        }
    }
}
