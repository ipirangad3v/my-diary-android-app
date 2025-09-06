package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.R.string.back
import digital.tonima.mydiary.R.string.new_entry
import digital.tonima.mydiary.R.string.save_entry
import digital.tonima.mydiary.R.string.write_here
import digital.tonima.mydiary.data.PasswordBasedCryptoManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    masterPassword: CharArray,
    onNavigateBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(new_entry)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            back
                        ))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (text.isNotBlank()) {
                            val fileName = "entry_${System.currentTimeMillis()}"
                            PasswordBasedCryptoManager.saveDiaryEntry(context, fileName, text, masterPassword)
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(save_entry))
                    }
                }
            )
        }
    ) { paddingValues ->
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            placeholder = { Text(stringResource(write_here)) },
        )
    }
}

