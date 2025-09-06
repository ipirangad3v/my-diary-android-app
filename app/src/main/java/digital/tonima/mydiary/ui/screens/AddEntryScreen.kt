package digital.tonima.mydiary.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import digital.tonima.mydiary.R.string.note_saved
import digital.tonima.mydiary.R.string.save_entry
import digital.tonima.mydiary.R.string.write_here
import digital.tonima.mydiary.data.PasswordBasedCryptoManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    masterPassword: CharArray,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var entryContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(new_entry)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (entryContent.isNotBlank()) {
                        try {
                            val filename = "entry_${System.currentTimeMillis()}.txt"
                            PasswordBasedCryptoManager.saveDiaryEntry(
                                context = context,
                                filename = filename,
                                content = entryContent,
                                masterPassword = masterPassword
                            )
                            Toast.makeText(
                                context,
                                context.getString(note_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                            onNavigateBack()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error saving entry: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Done, contentDescription = stringResource(save_entry))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TextField(
                value = entryContent,
                onValueChange = { entryContent = it },
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text(stringResource(write_here)) },
            )
        }
    }
}

