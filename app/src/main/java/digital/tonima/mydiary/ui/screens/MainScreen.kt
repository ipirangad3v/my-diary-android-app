package digital.tonima.mydiary.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import digital.tonima.mydiary.R
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import digital.tonima.mydiary.utils.formatTimestamp
import digital.tonima.mydiary.utils.getLocalDateFromFile
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    hasEntry: Boolean,
    onClick: (CalendarDay) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Isso torna a célula quadrada
            .padding(2.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) {
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                } else {
                    Color.Gray
                },
                fontSize = 14.sp
            )
            if (hasEntry && day.position == DayPosition.MonthDate) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            )
        }
    }
}
@Composable
private fun CalendarView(
    modifier: Modifier = Modifier,
    calendarState: com.kizitonwose.calendar.compose.CalendarState,
    selectedDate: LocalDate?,
    entryDates: Set<LocalDate>,
    onDateSelected: (LocalDate?) -> Unit
) {
    val monthTitle = remember(calendarState.firstVisibleMonth) {
        val year = calendarState.firstVisibleMonth.yearMonth.year
        val month = calendarState.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$month $year"
    }

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = monthTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
            val daysOfWeek = remember { firstDayOfWeekFromLocale().let {
                val days = DayOfWeek.entries.toTypedArray()
                days.slice(it.ordinal until days.size) + days.slice(0 until it.ordinal)
            }}
            DaysOfWeekTitle(daysOfWeek = daysOfWeek)
        }

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                Day(
                    day = day,
                    isSelected = selectedDate == day.date,
                    hasEntry = day.date in entryDates,
                    onClick = {
                        onDateSelected(if (selectedDate == it.date) null else it.date)
                    }
                )
            }
        )
    }
}

@Composable
private fun NotesListView(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    allDiaryEntries: List<File>,
    filteredEntries: List<File>,
    selectedDate: LocalDate?,
    onNoteClick: (File) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (allDiaryEntries.isEmpty()) {
            Text(stringResource(R.string.empty_notes_message), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        } else if (filteredEntries.isEmpty() && selectedDate != null) {
            Text("Nenhuma nota para este dia.", modifier = Modifier.padding(16.dp))
        } else if (filteredEntries.isEmpty()) {
            Text("Selecione um dia no calendário para ver as notas.", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredEntries) { file ->
                    EntryListItem(
                        file = file,
                        onClick = { onNoteClick(file) }
                    )
                }
            }
        }
    }
}

// --- TELA PRINCIPAL ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    masterPassword: CharArray,
    onAddEntry: () -> Unit,
    onLockRequest: () -> Unit
) {
    val context = LocalContext.current
    var allDiaryEntries by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedEntry by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current

    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val entryDates by remember(allDiaryEntries) {
        mutableStateOf(allDiaryEntries.mapNotNull { getLocalDateFromFile(it) }.toSet())
    }
    val filteredEntries by remember(allDiaryEntries, selectedDate) {
        mutableStateOf(
            allDiaryEntries.filter { file ->
                getLocalDateFromFile(file) == selectedDate
            }
        )
    }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    LaunchedEffect(Unit) {
        allDiaryEntries = PasswordBasedCryptoManager.getAllEntryFiles(context)
        isLoading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onAddEntry() }, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_note))
                }
                FloatingActionButton(onClick = { onLockRequest() }) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.lock_diary))
                }
            }
        }
    ) { innerPadding ->

        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    CalendarView(
                        modifier = Modifier.weight(0.45f),
                        calendarState = calendarState,
                        selectedDate = selectedDate,
                        entryDates = entryDates,
                        onDateSelected = { selectedDate = it }
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxHeight().width(1.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                    NotesListView(
                        modifier = Modifier.weight(0.55f),
                        isLoading = isLoading,
                        allDiaryEntries = allDiaryEntries,
                        filteredEntries = filteredEntries,
                        selectedDate = selectedDate,
                        onNoteClick = { file ->
                            val content = PasswordBasedCryptoManager.readDiaryEntry(context, file.name, masterPassword)
                            if (content != null) {
                                selectedEntry = Pair(file.name, content)
                            }
                        }
                    )
                }
            }
            else -> { // Portrait
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    CalendarView(
                        calendarState = calendarState,
                        selectedDate = selectedDate,
                        entryDates = entryDates,
                        onDateSelected = { selectedDate = it }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    NotesListView(
                        modifier = Modifier.weight(1f),
                        isLoading = isLoading,
                        allDiaryEntries = allDiaryEntries,
                        filteredEntries = filteredEntries,
                        selectedDate = selectedDate,
                        onNoteClick = { file ->
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

    if (selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { selectedEntry?.first?.let { Text(formatTimestamp(it)) } },
            text = { selectedEntry?.second?.let { Text(it) } },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text(stringResource(R.string.close))
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
                    Text(stringResource(R.string.delete))
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_deletion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let {
                            PasswordBasedCryptoManager.deleteDiaryEntry(context, it)
                            allDiaryEntries = PasswordBasedCryptoManager.getAllEntryFiles(context)
                        }
                        showDeleteConfirmationDialog = false
                        entryToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
