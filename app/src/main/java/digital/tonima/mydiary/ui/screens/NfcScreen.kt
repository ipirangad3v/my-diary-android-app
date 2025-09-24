package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import digital.tonima.mydiary.R
import digital.tonima.mydiary.R.string.nfc_secrets
import digital.tonima.mydiary.R.string.nfc_write_instructions
import digital.tonima.mydiary.R.string.waiting_for_nfc_tag
import digital.tonima.mydiary.R.string.write_secret_to_nfc
import digital.tonima.mydiary.R.string.your_secret
import digital.tonima.mydiary.ui.viewmodels.NfcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(
    masterPassword: CharArray,
    onWriteToTag: (ByteArray) -> Unit,
    viewModel: NfcViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isWaitingForTag && uiState.encryptedData != null) {
        onWriteToTag(uiState.encryptedData!!)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(nfc_secrets)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = CenterHorizontally
        ) {
            Text(
                text = stringResource(nfc_write_instructions),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.secretText,
                onValueChange = viewModel::onSecretTextChange,
                label = { Text(stringResource(your_secret)) },
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
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isWaitingForTag) {
                Column(
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(waiting_for_nfc_tag))
                    Button(onClick = viewModel::onWriteCancelled) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.prepareToWrite(masterPassword) },
                    enabled = uiState.secretText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(write_secret_to_nfc))
                }
            }
        }
    }
}
