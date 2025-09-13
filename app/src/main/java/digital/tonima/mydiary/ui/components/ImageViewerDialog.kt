package digital.tonima.mydiary.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.delete
import digital.tonima.mydiary.R.string.share
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerDialog(
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
                            Icon(Icons.Default.Close, contentDescription = stringResource(close))
                        }
                    },
                    actions = {
                        Row {
                            IconButton(onClick = onShareRequest) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(share))
                            }
                            IconButton(onClick = onDeleteRequest) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(delete))
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
                contentAlignment = Center
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
