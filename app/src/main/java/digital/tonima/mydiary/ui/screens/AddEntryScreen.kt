package digital.tonima.mydiary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import digital.tonima.mydiary.R
import digital.tonima.mydiary.R.string.back
import digital.tonima.mydiary.R.string.cancel
import digital.tonima.mydiary.R.string.content_required
import digital.tonima.mydiary.R.string.discard
import digital.tonima.mydiary.R.string.new_entry
import digital.tonima.mydiary.R.string.save_note
import digital.tonima.mydiary.R.string.your_thoughts
import digital.tonima.mydiary.ui.viewmodels.AddEntryEvent
import digital.tonima.mydiary.ui.viewmodels.AddEntryViewModel
import digital.tonima.mydiary.R.string.title as RTitle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    masterPassword: CharArray,
    onNavigateBack: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val fallbackTitle = stringResource(R.string.no_title)
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val handleBackNavigation = {
        if (content.isNotBlank()) {
            showConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Handle system back press
    BackHandler {
        handleBackNavigation()
    }

    // Listen for one-time events from the ViewModel (navigation, snackbars)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEntryEvent.NavigateBack -> onNavigateBack()
                is AddEntryEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text(stringResource(cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(new_entry)) },
                navigationIcon = {
                    IconButton(onClick = handleBackNavigation) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveEntry(
                                title = title,
                                content = content,
                                fallbackTitle = fallbackTitle,
                                masterPassword = masterPassword,
                                contentRequiredMessageResId = content_required
                            )
                        }
                    ) {
                        Text(stringResource(save_note))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(RTitle)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // Content field
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(your_thoughts)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
