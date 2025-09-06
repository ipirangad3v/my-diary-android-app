package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import digital.tonima.mydiary.R
import digital.tonima.mydiary.R.string.add_note
import digital.tonima.mydiary.R.string.app_name
import digital.tonima.mydiary.R.string.ask_for_lock_diary
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.error_decrypting
import digital.tonima.mydiary.R.string.lock_confirm
import digital.tonima.mydiary.R.string.lock_diary
import digital.tonima.mydiary.R.string.no

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    masterPassword: CharArray,
    onLockRequest: () -> Unit,
    onAddEntry: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(PasswordBasedCryptoManager.getAllEntryFiles(context)) }
    var showLockDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<File?>(null) }
    var decryptedContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = entries) {
        entries = PasswordBasedCryptoManager.getAllEntryFiles(context)
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text(stringResource(lock_diary)) },
            text = { Text(stringResource(ask_for_lock_diary)) },
            confirmButton = {
                TextButton(onClick = {
                    showLockDialog = false
                    onLockRequest()
                }) { Text(stringResource(lock_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) { Text(stringResource(no)) }
            }
        )
    }

    selectedEntry?.let { file ->
        decryptedContent = PasswordBasedCryptoManager.readDiaryEntry(context, file.name, masterPassword)
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(text = SimpleDateFormat.getDateTimeInstance().format(Date(file.name.removePrefix("entry_").toLong()))) },
            text = { Text(decryptedContent ?: stringResource(error_decrypting)) },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) { Text(stringResource(close)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(app_name)) },
                actions = {
                    IconButton(onClick = { showLockDialog = true }) {
                        Icon(Icons.Default.Lock, contentDescription = stringResource(lock_diary))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Default.Add, contentDescription = stringResource(add_note))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (entries.isEmpty()) {
                Text(stringResource(R.string.empty_notes_message))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(entries.sortedByDescending { it.name }) { file ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedEntry = file }) {
                            Text(
                                text = SimpleDateFormat.getDateTimeInstance().format(Date(file.name.removePrefix("entry_").toLong())),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

