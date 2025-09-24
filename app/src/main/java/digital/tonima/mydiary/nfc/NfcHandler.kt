package digital.tonima.mydiary.nfc

import android.content.Intent
import digital.tonima.mydiary.MainViewModel
import digital.tonima.mydiary.ui.viewmodels.NfcViewModel

interface NfcHandler {
    fun init(mainViewModel: MainViewModel, nfcViewModel: NfcViewModel)
    fun setupNfcForegroundDispatch()
    fun disableNfcForegroundDispatch()
    fun handleIntent(intent: Intent)
}
