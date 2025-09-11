package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import digital.tonima.mydiary.R.string.confirm_password
import digital.tonima.mydiary.R.string.create_master_password
import digital.tonima.mydiary.R.string.master_password
import digital.tonima.mydiary.R.string.master_password_description
import digital.tonima.mydiary.R.string.save_password
import digital.tonima.mydiary.ui.viewmodels.PasswordSetupEvent
import digital.tonima.mydiary.ui.viewmodels.PasswordSetupViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupScreen(
    viewModel: PasswordSetupViewModel = hiltViewModel(),
    onPasswordSet: (CharArray) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is PasswordSetupEvent.PasswordSet -> onPasswordSet(event.password)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(create_master_password), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(master_password_description), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(master_password)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.errorResId != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text(stringResource(confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.errorResId != null,
            modifier = Modifier.fillMaxWidth()
        )
        uiState.errorResId?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::onSavePasswordClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(save_password))
        }
    }
}
