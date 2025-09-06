package digital.tonima.meudiario.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import digital.tonima.meudiario.R
import digital.tonima.meudiario.data.CryptoManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onLockRequest: () -> Unit,
    onNavigateToAddEntry: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        entries = CryptoManager.getAllEntryFiles(context)
    }

    var showLockDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<File?>(null) }
    val showEntryDialog = selectedEntry != null

    if (showLockDialog) {
        LockDialog(
            onConfirm = {
                showLockDialog = false
                onLockRequest()
            },
            onDismiss = { showLockDialog = false }
        )
    }

    if (showEntryDialog) {
        val entryContent = remember(selectedEntry) {
            selectedEntry?.let { CryptoManager.readDiaryEntry(context, it.name) } ?: ""
        }
        EntryContentDialog(
            content = entryContent,
            onDismiss = { selectedEntry = null }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = onNavigateToAddEntry) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_note))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = { showLockDialog = true }) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(id = R.string.lock_diary))
                }
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.empty_notes_message))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries.sortedByDescending { it.lastModified() }) { entryFile ->
                    DiaryEntryItem(file = entryFile, onClick = { selectedEntry = it })
                }
            }
        }
    }
}

@Composable
fun DiaryEntryItem(file: File, onClick: (File) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(file) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val date = Date(file.lastModified())
            val formatter = remember {
                SimpleDateFormat("EEE, dd 'de' MMMM 'de' yyyy, HH:mm", Locale("pt", "BR"))
            }
            Text(
                text = formatter.format(date),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun LockDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.lock_diary)) },
        text = { Text(stringResource(id = R.string.ask_for_lock_diary)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(id = R.string.lock_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.no)) }
        }
    )
}

@Composable
fun EntryContentDialog(content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.your_note)) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}

