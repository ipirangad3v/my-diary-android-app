package digital.tonima.mydiary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import digital.tonima.mydiary.R.string.cancel
import digital.tonima.mydiary.R.string.confirm_deletion_message
import digital.tonima.mydiary.R.string.confirm_deletion_title
import digital.tonima.mydiary.R.string.content_required
import digital.tonima.mydiary.R.string.discard
import digital.tonima.mydiary.R.string.discard_changes_message
import digital.tonima.mydiary.R.string.discard_changes_title
import digital.tonima.mydiary.R.string.no_title
import digital.tonima.mydiary.ui.components.AddEntryTopBar
import digital.tonima.mydiary.ui.components.ConfirmationDialog
import digital.tonima.mydiary.ui.components.FormattingToolbar
import digital.tonima.mydiary.ui.viewmodels.AddEntryEvent
import digital.tonima.mydiary.ui.viewmodels.AddEntryViewModel
import kotlinx.coroutines.flow.collectLatest
import digital.tonima.mydiary.R.string.title as RTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    masterPassword: CharArray,
    entryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fallbackTitle = stringResource(no_title)
    val context = LocalContext.current

    var title by remember(uiState.initialTitle) { mutableStateOf(uiState.initialTitle) }
    val richTextState = rememberRichTextState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBackConfirmationDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(entryId) {
        viewModel.initialize(entryId, masterPassword)
    }

    LaunchedEffect(uiState.initialContentHtml) {
        if (uiState.initialContentHtml.isNotEmpty()) {
            richTextState.setHtml(uiState.initialContentHtml)
        }
    }

    val handleBackNavigation = {
        val hasChanges = if (entryId != null) {
            title != uiState.initialTitle || richTextState.toHtml() != uiState.initialContentHtml
        } else {
            title.isNotBlank() || richTextState.annotatedString.isNotBlank()
        }

        if (hasChanges) {
            showBackConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBackNavigation() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEntryEvent.NavigateBack -> onNavigateBack()
                is AddEntryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    if (showBackConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmationDialog = false },
            title = { Text(stringResource(discard_changes_title)) },
            text = { Text(stringResource(discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirmationDialog = false
                    onNavigateBack()
                }) { Text(stringResource(discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmationDialog = false }) { Text(stringResource(cancel)) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AddEntryTopBar(
                entryId = entryId,
                onBackClick = handleBackNavigation,
                onDeleteClick = viewModel::onDeleteRequest,
                onSaveClick = {
                    viewModel.saveEntry(
                        title = title,
                        contentHtml = richTextState.toHtml(),
                        fallbackTitle = fallbackTitle,
                        masterPassword = masterPassword,
                        contentRequiredMessageResId = content_required
                    )
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(RTitle)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    )
                )

                FormattingToolbar(richTextState = richTextState)

                RichTextEditor(
                    state = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    keyboardOptions = KeyboardOptions(
                        imeAction = Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    )
                )
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(confirm_deletion_title),
            text = stringResource(confirm_deletion_message),
            onConfirm = { viewModel.deleteEntry(masterPassword) },
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }
}
