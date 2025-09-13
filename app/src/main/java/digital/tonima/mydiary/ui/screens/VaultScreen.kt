package digital.tonima.mydiary.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import digital.tonima.mydiary.R
import digital.tonima.mydiary.imagevault.EncryptedImageFetcher
import digital.tonima.mydiary.ui.components.ConfirmationDialog
import digital.tonima.mydiary.ui.viewmodels.VaultEvent
import digital.tonima.mydiary.ui.viewmodels.VaultViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@Composable
fun VaultScreen(
    masterPassword: CharArray,
    onAddImage: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imageLoader = remember(masterPassword) {
        ImageLoader.Builder(context)
            .components {
                add(EncryptedImageFetcher.Factory(context, masterPassword))
            }
            .build()
    }

    LaunchedEffect(Unit) {
        viewModel.loadVaultImages()

        // Listen for one-time share events
        viewModel.eventFlow.collectLatest { event ->
            when(event) {
                is VaultEvent.ShareImage -> {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        type = "image/jpeg"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_image)))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.vaultImages.isEmpty()) {
            Text(
                text = stringResource(R.string.vault_empty_message),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(uiState.vaultImages, key = { it.name }) { file ->
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
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_image))
        }
    }

    uiState.selectedImage?.let { file ->
        ImageViewerDialog(
            file = file,
            imageLoader = imageLoader,
            onDismiss = viewModel::onDismissImageViewer,
            onDeleteRequest = viewModel::onDeleteRequest,
            onShareRequest = { viewModel.onShareRequest(masterPassword) }
        )
    }

    if (uiState.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_image_title),
            text = stringResource(R.string.delete_image_message),
            onConfirm = viewModel::deleteSelectedImage,
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageViewerDialog(
    file: File,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit,
    onShareRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        Row {
                            IconButton(onClick = onShareRequest) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                            }
                            IconButton(onClick = onDeleteRequest) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = file,
                    imageLoader = imageLoader,
                    contentDescription = "Full screen encrypted image",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
