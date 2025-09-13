package digital.tonima.mydiary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import digital.tonima.mydiary.R.string.diary
import digital.tonima.mydiary.R.string.vault
import digital.tonima.mydiary.ui.screens.BottomBarScreen
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Diary

@Composable
fun AppBottomNavigation(
    currentScreen: BottomBarScreen,
    onScreenSelected: (BottomBarScreen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(diary)) },
            label = { Text(stringResource(diary)) },
            selected = currentScreen == Diary,
            onClick = { onScreenSelected(Diary) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Lock, contentDescription = stringResource(vault)) },
            label = { Text(stringResource(vault)) },
            selected = currentScreen == BottomBarScreen.Vault,
            onClick = { onScreenSelected(BottomBarScreen.Vault) }
        )
    }
}
