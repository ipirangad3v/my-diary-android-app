package digital.tonima.mydiary.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.R.string.add_note
import digital.tonima.mydiary.R.string.app_name
import digital.tonima.mydiary.R.string.ask_for_lock_diary
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.empty_notes_message
import digital.tonima.mydiary.R.string.lock_diary
import digital.tonima.mydiary.R.string.lock_confirm
import digital.tonima.mydiary.R.string.no
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    masterPassword: CharArray,
    onLockRequest: () -> Unit,
    onAddEntry: () -> Unit
) {
    val context = LocalContext.current
    var diaryEntries by remember { mutableStateOf<List<File>>(emptyList()) }
    var showLockDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<File?>(null) }
    var decryptedContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        diaryEntries = PasswordBasedCryptoManager.getAllEntryFiles(context)
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text(stringResource(lock_diary)) },
            text = { Text(stringResource(ask_for_lock_diary)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLockDialog = false
                        onLockRequest()
                    }
                ) {
                    Text(stringResource(lock_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLockDialog = false }
                ) {
                    Text(stringResource(no))
                }
            }
        )
    }

    selectedEntry?.let { entryFile ->
        try {
            decryptedContent = PasswordBasedCryptoManager.readDiaryEntry(context, masterPassword, entryFile.name)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao ler a entrada: ${e.message}", Toast.LENGTH_LONG).show()
            decryptedContent = null
            selectedEntry = null // Reset selection on error
        }

        decryptedContent?.let { content ->
            AlertDialog(
                onDismissRequest = { selectedEntry = null; decryptedContent = null },
                title = { Text(formatTimestamp(entryFile.name)) },
                text = { Text(content) },
                confirmButton = {
                    TextButton(onClick = { selectedEntry = null; decryptedContent = null }) {
                        Text(stringResource(close))
                    }
                }
            )
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(app_name)) },
                actions = {
                    IconButton(onClick = { showLockDialog = true }) {
                        Icon(Icons.Filled.Lock, contentDescription = stringResource(lock_diary))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(add_note))
            }
        }
    ) { innerPadding ->
        if (diaryEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(empty_notes_message))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(diaryEntries) { file ->
                    DiaryEntryCard(file = file, onClick = { selectedEntry = file })
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryCard(file: File, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatTimestamp(file.name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimestamp(filename: String): String {
    return try {
        val timestamp = filename.removePrefix("entry_").removeSuffix(".txt").toLong()
        val formatter = DateFormat.getDateTimeInstance()
        formatter.format(Date(timestamp))
    } catch (_: Exception) {
        "Data Inválida"
    }
}

