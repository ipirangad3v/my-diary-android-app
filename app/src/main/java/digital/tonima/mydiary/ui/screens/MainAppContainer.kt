package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mydiary.MainViewModel
import digital.tonima.mydiary.ui.components.AppBottomNavigation
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Diary
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Nfc
import digital.tonima.mydiary.ui.screens.BottomBarScreen.Vault

@Composable
fun MainAppContainer(
    mainViewModel: MainViewModel,
    masterPassword: CharArray,
    onAddImage: () -> Unit,
    onReauthenticate: (titleResId: Int, subtitleResId: Int, action: () -> Unit) -> Unit,
    onPurchaseRequest: () -> Unit,
    onEditEntry: (fileName: String) -> Unit,
    hasNfcSupport: Boolean = false,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val principalScreenState = uiState as? AppScreen.Principal ?: return

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                currentScreen = principalScreenState.currentScreen,
                onScreenSelected = mainViewModel::onScreenSelected,
                hasNfcSupport = hasNfcSupport
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (principalScreenState.currentScreen) {
                Diary -> PrincipalScreen(
                    masterPassword = masterPassword,
                    onAddEntry = { mainViewModel.navigateToAddEntry() },
                    onEditEntry = onEditEntry,
                    onLockRequest = mainViewModel::lockApp,
                    onResetApp = mainViewModel::resetApp,
                    onReauthenticate = onReauthenticate,
                    onPurchaseRequest = onPurchaseRequest
                )

                Vault -> VaultScreen(
                    masterPassword = masterPassword,
                    onAddImage = onAddImage
                )

                Nfc ->
                    NfcScreen(
                        masterPassword = masterPassword,
                        onWriteToTag = { data -> mainViewModel.onNfcTagRead(data) }
                    )
            }
        }
    }
}
