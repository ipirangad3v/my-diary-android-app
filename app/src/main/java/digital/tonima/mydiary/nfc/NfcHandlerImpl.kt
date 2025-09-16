package digital.tonima.mydiary.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.scopes.ActivityScoped
import digital.tonima.mydiary.MainViewModel
import digital.tonima.mydiary.R
import digital.tonima.mydiary.ui.viewmodels.NfcViewModel
import java.io.IOException
import javax.inject.Inject

private const val MIME_TYPE = "application/vnd.digital.tonima.mydiary.secret"

@BindType(installIn = BindType.Component.ACTIVITY, to = NfcHandler::class)
@ActivityScoped
class NfcHandlerImpl @Inject constructor(
    private val activity: FragmentActivity,
    private val nfcAdapter: NfcAdapter?
) : NfcHandler {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var nfcViewModel: NfcViewModel

    /**
     * Inicializa o gestor com os ViewModels necessários.
     * Isto deve ser chamado a partir da Activity depois de os ViewModels serem criados.
     */
    override fun init(mainViewModel: MainViewModel, nfcViewModel: NfcViewModel) {
        this.mainViewModel = mainViewModel
        this.nfcViewModel = nfcViewModel
    }

    override fun setupNfcForegroundDispatch() {
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )
        val intentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            intentFilter.addDataType(MIME_TYPE)
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Failed to add MIME type.", e)
        }
        val intentFiltersArray = arrayOf(intentFilter)
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, null)
    }

    override fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    override fun handleIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED != intent.action) return

        val nfcUiState = nfcViewModel.uiState.value
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (nfcUiState.isWaitingForTag && nfcUiState.encryptedData != null) {
            writeTag(tag, nfcUiState.encryptedData)
        } else {
            readTag(intent)
        }
    }

    private fun writeTag(tag: Tag?, data: ByteArray) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Toast.makeText(activity, "Tag is not NDEF-formatted.", Toast.LENGTH_SHORT).show()
            return
        }
        val record = NdefRecord.createMime(MIME_TYPE, data)
        val message = NdefMessage(arrayOf(record))

        try {
            ndef.connect()
            ndef.writeNdefMessage(message)
            Toast.makeText(activity, activity.getString(R.string.nfc_write_success), Toast.LENGTH_SHORT).show()
            nfcViewModel.onTagWritten()
        } catch (e: IOException) {
            Toast.makeText(activity, activity.getString(R.string.nfc_write_error), Toast.LENGTH_LONG).show()
            Log.e(
                "NfcHandler",
                "Failed to write data to NFC tag. Is the tag too small? ${e.message}",
                e
            )
            nfcViewModel.onWriteCancelled()
        } finally {
            try {
                ndef.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun readTag(intent: Intent) {
        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        (messages?.firstOrNull() as? NdefMessage)?.records?.firstOrNull()?.let { record ->
            if (record.toMimeType() == MIME_TYPE) {
                mainViewModel.onNfcTagRead(record.payload)
            }
        }
    }
}
