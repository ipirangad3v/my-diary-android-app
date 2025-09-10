package digital.tonima.mydiary.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
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
import digital.tonima.mydiary.BuildConfig.ADMOB_BANNER_AD_UNIT_HOME
import digital.tonima.mydiary.R.string.add_note
import digital.tonima.mydiary.R.string.app_name
import digital.tonima.mydiary.R.string.cancel
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.confirm
import digital.tonima.mydiary.R.string.confirm_deletion_message
import digital.tonima.mydiary.R.string.confirm_deletion_title
import digital.tonima.mydiary.R.string.delete
import digital.tonima.mydiary.R.string.empty_notes_message
import digital.tonima.mydiary.R.string.loading
import digital.tonima.mydiary.R.string.lock_diary
import digital.tonima.mydiary.R.string.none_note_for_this_day
import digital.tonima.mydiary.R.string.select_a_day_to_see_notes
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.ui.components.AdBannerView
import digital.tonima.mydiary.ui.components.EntryListItem
import digital.tonima.mydiary.utils.formatTimestampToHourAndMinute
import digital.tonima.mydiary.utils.getLocalDateFromFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            .aspectRatio(1f)
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
            Text(
                text = monthTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val daysOfWeek = remember {
                firstDayOfWeekFromLocale().let {
                    val days = DayOfWeek.entries.toTypedArray()
                    days.slice(it.ordinal until days.size) + days.slice(0 until it.ordinal)
                }
            }
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
    decryptedEntries: Map<File, DiaryEntry>,
    selectedDate: LocalDate?,
    onNoteClick: (File, DiaryEntry) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (allDiaryEntries.isEmpty()) {
            Text(
                stringResource(empty_notes_message),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else if (filteredEntries.isEmpty() && selectedDate != null) {
            Text(stringResource(none_note_for_this_day), modifier = Modifier.padding(16.dp))
        } else if (filteredEntries.isEmpty()) {
            Text(
                stringResource(select_a_day_to_see_notes),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                items(filteredEntries) { file ->
                    val entry = decryptedEntries[file]
                    if (entry != null) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    masterPassword: CharArray,
    onAddEntry: () -> Unit,
    onLockRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allDiaryFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var decryptedEntries by remember { mutableStateOf<Map<File, DiaryEntry>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedEntry by remember { mutableStateOf<Pair<File, DiaryEntry?>?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val entryDates by remember(allDiaryFiles) {
        mutableStateOf(allDiaryFiles.mapNotNull { getLocalDateFromFile(it) }.toSet())
    }
    val filteredEntries by remember(allDiaryFiles, selectedDate) {
        mutableStateOf(
            allDiaryFiles.filter { file ->
                getLocalDateFromFile(file) == selectedDate
            }.sortedByDescending { it.name }
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
        isLoading = true
        withContext(Dispatchers.IO) {
            val files = PasswordBasedCryptoManager.getAllEntryFiles(context)
            val decrypted = files.mapNotNull { file ->
                PasswordBasedCryptoManager.readDiaryEntry(context, file, masterPassword)?.let { entry ->
                    file to entry
                }
            }.toMap()
            allDiaryFiles = files
            decryptedEntries = decrypted
        }
        isLoading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(app_name)) }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onAddEntry() }, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(add_note))
                }
                FloatingActionButton(onClick = { onLockRequest() }) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(lock_diary))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AdBannerView(adId = ADMOB_BANNER_AD_UNIT_HOME)
            Box(Modifier.weight(1f)) {
                when (configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            CalendarView(
                                modifier = Modifier.weight(0.45f),
                                calendarState = calendarState,
                                selectedDate = selectedDate,
                                entryDates = entryDates,
                                onDateSelected = { selectedDate = it }
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )
                            NotesListView(
                                modifier = Modifier.weight(0.55f),
                                isLoading = isLoading,
                                allDiaryEntries = allDiaryFiles,
                                filteredEntries = filteredEntries,
                                decryptedEntries = decryptedEntries,
                                selectedDate = selectedDate,
                                onNoteClick = { file, entry -> selectedEntry = file to entry }
                            )
                        }
                    }

                    else -> { // Portrait
                        Column(modifier = Modifier.fillMaxSize()) {
                            CalendarView(
                                calendarState = calendarState,
                                selectedDate = selectedDate,
                                entryDates = entryDates,
                                onDateSelected = { selectedDate = it }
                            )
                            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                            NotesListView(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                isLoading = isLoading,
                                allDiaryEntries = allDiaryFiles,
                                filteredEntries = filteredEntries,
                                decryptedEntries = decryptedEntries,
                                selectedDate = selectedDate,
                                onNoteClick = { file, entry -> selectedEntry = file to entry }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedEntry != null) {
        val (_, entry) = selectedEntry!!
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry?.title ?: stringResource(loading)) },
            text = {
                if (entry != null) {
                    Text(entry.content)
                } else {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text(stringResource(close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = true
                    }
                ) {
                    Text(stringResource(delete), color = MaterialTheme.colorScheme.error)
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
                        scope.launch {
                            selectedEntry?.first?.let { fileToDelete ->
                                withContext(Dispatchers.IO) {
                                    PasswordBasedCryptoManager.deleteDiaryEntry(fileToDelete)
                                    val files = PasswordBasedCryptoManager.getAllEntryFiles(context)
                                    val decrypted = files.mapNotNull { file ->
                                        PasswordBasedCryptoManager.readDiaryEntry(context, file, masterPassword)
                                            ?.let { entry ->
                                                file to entry
                                            }
                                    }.toMap()
                                    allDiaryFiles = files
                                    decryptedEntries = decrypted
                                }
                            }
                            showDeleteConfirmationDialog = false
                            selectedEntry = null
                        }
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
