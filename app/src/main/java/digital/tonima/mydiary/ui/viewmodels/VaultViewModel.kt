package digital.tonima.mydiary.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.database.entities.VaultImageEntity
import digital.tonima.mydiary.database.repositories.VaultRepository
import digital.tonima.mydiary.delegates.ProUserProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class VaultUiState(
    val isLoading: Boolean = true,
    val vaultImages: List<VaultImageEntity> = emptyList(),
    val selectedImage: VaultImageEntity? = null,
    val showDeleteConfirmation: Boolean = false
)

sealed class VaultEvent {
    data class ShareImage(val uri: Uri) : VaultEvent()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    proUserProvider: ProUserProvider
) : ViewModel(), ProUserProvider by proUserProvider {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VaultEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var masterPassword: CharArray? = null

    fun initialize(password: CharArray) {
        if (masterPassword == null) {
            masterPassword = password
            loadVaultImages()
        }
    }

    fun loadVaultImages() {
        val currentPassword = masterPassword ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            vaultRepository.getImages(currentPassword).collectLatest { images ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        vaultImages = images
                    )
                }
            }
        }
    }

    fun saveImage(uri: Uri) {
        val currentPassword = masterPassword ?: return
        viewModelScope.launch(Dispatchers.IO) {
            vaultRepository.saveImage(uri, currentPassword)
        }
    }

    fun onImageClicked(image: VaultImageEntity) {
        _uiState.update { it.copy(selectedImage = image) }
    }

    fun onDismissImageViewer() {
        _uiState.update { it.copy(selectedImage = null) }
    }

    fun onDeleteRequest() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteSelectedImage() {
        val imageToDelete = _uiState.value.selectedImage ?: return
        val currentPassword = masterPassword ?: return
        viewModelScope.launch(Dispatchers.IO) {
            vaultRepository.deleteImage(imageToDelete, currentPassword)
        }
        _uiState.update { it.copy(selectedImage = null, showDeleteConfirmation = false) }
    }

    fun onShareRequest() {
        val imageToShare = _uiState.value.selectedImage ?: return
        val currentPassword = masterPassword ?: return
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                vaultRepository.prepareImageForSharing(imageToShare, currentPassword)
            }
            uri?.let {
                _eventFlow.emit(VaultEvent.ShareImage(it))
            }
        }
    }
}
