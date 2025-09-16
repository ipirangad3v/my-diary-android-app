package digital.tonima.mydiary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import digital.tonima.mydiary.R.drawable.nfc
import digital.tonima.mydiary.R.string.diary
import digital.tonima.mydiary.R.string.nfc_secrets
import digital.tonima.mydiary.R.string.vault
import digital.tonima.mydiary.ui.screens.BottomBarScreen
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Diary
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Nfc
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Vault

@Composable
fun AppBottomNavigation(
    currentScreen: BottomBarScreen,
    onScreenSelected: (BottomBarScreen) -> Unit,
    hasNfcSupport: Boolean = false
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
            selected = currentScreen == Vault,
            onClick = { onScreenSelected(Vault) }
        )
        if (hasNfcSupport) {
            NavigationBarItem(
                icon = { Icon(painterResource(nfc), contentDescription = stringResource(nfc_secrets)) },
                label = { Text(stringResource(nfc_secrets)) },
                selected = currentScreen == Nfc,
                onClick = { onScreenSelected(Nfc) }
            )
        }


    }
}
