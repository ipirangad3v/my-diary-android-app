package digital.tonima.mydiary.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.R
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.utils.formatTimestampToHourAndMinute
import java.io.File
import java.time.LocalDate

@Composable
internal fun NotesListView(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    allEntriesCount: Int,
    filteredEntries: List<Pair<File, DiaryEntry>>,
    selectedDate: LocalDate?,
    onNoteClick: (File, DiaryEntry) -> Unit
) {
    Box(modifier = modifier.padding(8.dp), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (allEntriesCount == 0) {
            Text(
                stringResource(R.string.empty_notes_message),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else if (filteredEntries.isEmpty() && selectedDate != null) {
            Text(stringResource(R.string.none_note_for_this_day), modifier = Modifier.padding(16.dp))
        } else if (filteredEntries.isEmpty()) {
            Text(
                stringResource(R.string.select_a_day_to_see_notes),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredEntries, key = { it.first.name }) { (file, entry) ->
                    EntryListItem(
                        title = entry.title,
                        time = formatTimestampToHourAndMinute(file.name),
                        onClick = { onNoteClick(file, entry) }
                    )
                }
            }
        }
    }
}
