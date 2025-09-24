package digital.tonima.mydiary.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import digital.tonima.mydiary.BuildConfig.ADMOB_BANNER_AD_UNIT_VAULT_SCREEN
import digital.tonima.mydiary.R.string.add_image
import digital.tonima.mydiary.R.string.delete_image_message
import digital.tonima.mydiary.R.string.delete_image_title
import digital.tonima.mydiary.R.string.share_image
import digital.tonima.mydiary.R.string.vault_empty_message
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import digital.tonima.mydiary.imagevault.EncryptedImageFetcher
import digital.tonima.mydiary.ui.components.AdBannerView
import digital.tonima.mydiary.ui.components.ConfirmationDialog
import digital.tonima.mydiary.ui.components.ImageViewerDialog
import digital.tonima.mydiary.ui.viewmodels.VaultEvent
import digital.tonima.mydiary.ui.viewmodels.VaultViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VaultScreen(
    masterPassword: CharArray,
    cryptoManager: PasswordBasedCryptoManager,
    onAddImage: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val isProUser by viewModel.isProUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imageLoader = remember(masterPassword) {
        ImageLoader.Builder(context)
            .components {
                add(EncryptedImageFetcher.Factory(context, masterPassword, cryptoManager))
            }
            .build()
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(masterPassword)

        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is VaultEvent.ShareImage -> {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        type = "image/jpeg"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(share_image)))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Center))
        } else if (uiState.vaultImages.isEmpty()) {
            Text(
                text = stringResource(vault_empty_message),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Center)
                    .padding(16.dp)
            )
        } else {
            AdBannerView(
                adId = ADMOB_BANNER_AD_UNIT_VAULT_SCREEN,
                isProUser = isProUser
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = spacedBy(4.dp),
                horizontalArrangement = spacedBy(4.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(uiState.vaultImages, key = { it.encryptedFileName }) { file ->
                    AsyncImage(
                        model = file,
                        imageLoader = imageLoader,
                        contentDescription = "Encrypted image",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { viewModel.onImageClicked(file) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddImage,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(add_image))
        }
    }

    uiState.selectedImage?.let { imageEntity ->
        ImageViewerDialog(
            imageEntity = imageEntity,
            imageLoader = imageLoader,
            onDismiss = viewModel::onDismissImageViewer,
            onDeleteRequest = viewModel::onDeleteRequest,
            onShareRequest = { viewModel.onShareRequest() }
        )
    }

    if (uiState.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(delete_image_title),
            text = stringResource(delete_image_message),
            onConfirm = viewModel::deleteSelectedImage,
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }
}
