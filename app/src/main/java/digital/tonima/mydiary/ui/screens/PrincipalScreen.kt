package digital.tonima.mydiary.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import digital.tonima.mydiary.BuildConfig.ADMOB_BANNER_AD_UNIT_HOME
import digital.tonima.mydiary.R
import digital.tonima.mydiary.R.string.add_note
import digital.tonima.mydiary.R.string.close
import digital.tonima.mydiary.R.string.confirm_deletion_message
import digital.tonima.mydiary.R.string.confirm_deletion_title
import digital.tonima.mydiary.R.string.delete
import digital.tonima.mydiary.R.string.delete_all_confirmation_message
import digital.tonima.mydiary.R.string.delete_all_confirmation_title
import digital.tonima.mydiary.R.string.lock_diary
import digital.tonima.mydiary.R.string.menu
import digital.tonima.mydiary.R.string.re_authentication_subtitle
import digital.tonima.mydiary.R.string.re_authentication_title
import digital.tonima.mydiary.R.string.reset_app_confirmation_message
import digital.tonima.mydiary.R.string.reset_app_confirmation_title
import digital.tonima.mydiary.R.string.upgrade_confirmation_message
import digital.tonima.mydiary.R.string.upgrade_confirmation_title
import digital.tonima.mydiary.ui.components.AdBannerView
import digital.tonima.mydiary.ui.components.CalendarView
import digital.tonima.mydiary.ui.components.ConfirmationDialog
import digital.tonima.mydiary.ui.components.DrawerContent
import digital.tonima.mydiary.ui.components.NotesListView
import digital.tonima.mydiary.ui.viewmodels.PrincipalViewModel
import digital.tonima.mydiary.utils.getLocalDateFromFile
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalScreen(
    masterPassword: CharArray,
    onAddEntry: () -> Unit,
    onLockRequest: () -> Unit,
    onResetApp: () -> Unit,
    onReauthenticate: (titleResId: Int, subtitleResId: Int, action: () -> Unit) -> Unit,
    onPurchaseRequest: () -> Unit,
    viewModel: PrincipalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isProUser by viewModel.isProUser.collectAsState()
    val configuration = LocalConfiguration.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val entryDates by remember(uiState.allDecryptedEntries) {
        mutableStateOf(uiState.allDecryptedEntries.keys.mapNotNull { getLocalDateFromFile(it) }.toSet())
    }
    val filteredEntries by remember(uiState.allDecryptedEntries, uiState.selectedDate) {
        mutableStateOf(
            uiState.allDecryptedEntries.filter { (file, _) ->
                getLocalDateFromFile(file) == uiState.selectedDate
            }.toList().sortedByDescending { it.first.name }
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
        viewModel.loadEntries(masterPassword)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                isProUser = isProUser,
                onDeleteAll = viewModel::onDeleteAllRequest,
                onResetApp = viewModel::onResetAppRequest,
                onUpgradeToPro = viewModel::onUpgradeToProRequest,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(menu))
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(onClick = onAddEntry, modifier = Modifier.padding(bottom = 16.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(add_note))
                    }
                    FloatingActionButton(onClick = onLockRequest) {
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
                Box(Modifier.weight(1f)) {
                    when (configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                CalendarView(
                                    modifier = Modifier.weight(0.45f),
                                    calendarState = calendarState,
                                    selectedDate = uiState.selectedDate,
                                    entryDates = entryDates,
                                    onDateSelected = viewModel::onDateSelected
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
                                    isLoading = uiState.isLoading,
                                    allEntriesCount = uiState.allDecryptedEntries.size,
                                    filteredEntries = filteredEntries,
                                    selectedDate = uiState.selectedDate,
                                    onNoteClick = viewModel::onEntryClicked
                                )
                            }
                        }

                        else -> { // Portrait
                            Column(modifier = Modifier.fillMaxSize()) {
                                CalendarView(
                                    calendarState = calendarState,
                                    selectedDate = uiState.selectedDate,
                                    entryDates = entryDates,
                                    onDateSelected = viewModel::onDateSelected
                                )
                                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                                NotesListView(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    isLoading = uiState.isLoading,
                                    allEntriesCount = uiState.allDecryptedEntries.size,
                                    filteredEntries = filteredEntries,
                                    selectedDate = uiState.selectedDate,
                                    onNoteClick = viewModel::onEntryClicked
                                )
                            }
                        }
                    }
                }
                AdBannerView(
                    adId = ADMOB_BANNER_AD_UNIT_HOME,
                    isProUser = isProUser
                )

            }
        }
    }

    uiState.selectedEntry?.let { (_, entry) ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissEntryDialog,
            title = { Text(entry.title) },
            text = { Text(entry.content) },
            confirmButton = {
                TextButton(onClick = viewModel::onDismissEntryDialog) {
                    Text(stringResource(close))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteRequest) {
                    Text(stringResource(delete), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (uiState.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(confirm_deletion_title),
            text = stringResource(confirm_deletion_message),
            onConfirm = {
                onReauthenticate(
                    re_authentication_title,
                    re_authentication_subtitle,
                    { viewModel.deleteSelectedEntry(masterPassword) }
                )
            },
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }

    if (uiState.showDeleteAllConfirmation) {
        ConfirmationDialog(
            title = stringResource(delete_all_confirmation_title),
            text = stringResource(delete_all_confirmation_message),
            onConfirm = {
                onReauthenticate(
                    re_authentication_title,
                    re_authentication_subtitle,
                    { viewModel.deleteAllEntries(masterPassword) }
                )
            },
            onDismiss = viewModel::onDismissDeleteAllDialog
        )
    }

    if (uiState.showResetAppConfirmation) {
        ConfirmationDialog(
            title = stringResource(reset_app_confirmation_title),
            text = stringResource(reset_app_confirmation_message),
            onConfirm = {
                onReauthenticate(
                    re_authentication_title,
                    re_authentication_subtitle,
                    onResetApp
                )
            },
            onDismiss = viewModel::onDismissResetAppDialog
        )
    }

    if (uiState.showUpgradeConfirmation) {
        ConfirmationDialog(
            title = stringResource(upgrade_confirmation_title),
            text = stringResource(upgrade_confirmation_message),
            onConfirm = {
                viewModel.onDismissUpgradeDialog()
                onPurchaseRequest()
            },
            onDismiss = viewModel::onDismissUpgradeDialog
        )
    }
}
