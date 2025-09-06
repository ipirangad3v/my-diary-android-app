package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.R.string.add_note
import digital.tonima.mydiary.R.string.app_name
import digital.tonima.mydiary.R.string.cancel
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.confirm
import digital.tonima.mydiary.R.string.confirm_deletion_message
import digital.tonima.mydiary.R.string.confirm_deletion_title
import digital.tonima.mydiary.R.string.delete
import digital.tonima.mydiary.R.string.empty_notes_message
import digital.tonima.mydiary.R.string.lock_diary
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import java.io.File
import java.text.DateFormat.getDateTimeInstance
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    masterPassword: CharArray,
    onAddEntry: () -> Unit,
    onLockRequest: () -> Unit
) {
    val context = LocalContext.current
    var diaryEntries by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedEntry by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        diaryEntries = PasswordBasedCryptoManager.getAllEntryFiles(context)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(app_name)) }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { onAddEntry() },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(add_note))
                }
                FloatingActionButton(onClick = { onLockRequest() }) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(lock_diary))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (diaryEntries.isEmpty()) {
                Text(stringResource(empty_notes_message))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(diaryEntries) { file ->
                        EntryListItem(
                            file = file,
                            onClick = {
                                val content = PasswordBasedCryptoManager.readDiaryEntry(context, file.name, masterPassword)
                                if (content != null) {
                                    selectedEntry = Pair(file.name, content)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(formatTimestamp(selectedEntry!!.first)) },
            text = { Text(selectedEntry!!.second) },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text(stringResource(close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        entryToDelete = selectedEntry?.first
                        showDeleteConfirmationDialog = true
                        selectedEntry = null
                    }
                ) {
                    Text(stringResource(delete))
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(stringResource(confirm_deletion_title)) },
            text = { Text(stringResource(confirm_deletion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let {
                            PasswordBasedCryptoManager.deleteDiaryEntry(context, it)
                            diaryEntries = PasswordBasedCryptoManager.getAllEntryFiles(context)
                        }
                        showDeleteConfirmationDialog = false
                        entryToDelete = null
                    }
                ) {
                    Text(stringResource(confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text(stringResource(cancel))
                }
            }
        )
    }
}

@Composable
private fun EntryListItem(file: File, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(formatTimestamp(file.name)) }
        )
    }
}

private fun formatTimestamp(filename: String): String {
    return try {
        val timestamp = filename.removePrefix("entry_").removeSuffix(".txt").toLong()
        val dateFormat = getDateTimeInstance()
        dateFormat.format(Date(timestamp))
    } catch (_: Exception) {
        "Data Inválida"
    }
}

