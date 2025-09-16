package digital.tonima.mydiary

import android.content.Intent
import android.content.pm.PackageManager.FEATURE_NFC
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mydiary.billing.BillingManager
import digital.tonima.mydiary.biometrics.BiometricAuthManager
import digital.tonima.mydiary.nfc.NfcHandler
import digital.tonima.mydiary.ui.screens.AddEntryScreen
import digital.tonima.mydiary.ui.screens.AppScreen
import digital.tonima.mydiary.ui.screens.LockedScreen
import digital.tonima.mydiary.ui.screens.MainAppContainer
import digital.tonima.mydiary.ui.screens.ManualPasswordScreen
import digital.tonima.mydiary.ui.screens.PasswordSetupScreen
import digital.tonima.mydiary.ui.theme.MyDiaryTheme
import digital.tonima.mydiary.ui.viewmodels.NfcViewModel
import digital.tonima.mydiary.ui.viewmodels.VaultViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val hasNfcSupport: Boolean by lazy {
        packageManager.hasSystemFeature(FEATURE_NFC)
    }

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var nfcHandler: NfcHandler

    private val viewModel: MainViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

    private val enrollLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.retryBiometricAction()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager.connect()
        enableEdgeToEdge()
        nfcHandler.init(viewModel, nfcViewModel)

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                (viewModel.uiState.value as? AppScreen.Principal)?.masterPassword?.let { password ->
                    vaultViewModel.saveImage(uri, password)
                }
            }
        }

        setContent {
            MyDiaryTheme {
                val screen by viewModel.uiState.collectAsStateWithLifecycle()

                when (val currentScreen = screen) {
                    is AppScreen.SetupPassword -> {
                        PasswordSetupScreen(onPasswordSet = { password ->
                            biometricAuthManager.authenticateForEncryption(
                                onSuccess = { cipher -> viewModel.onPasswordSetup(password, cipher) },
                                onEnrollmentRequired = { actionToRetry ->
                                    viewModel.setPendingBiometricAction(actionToRetry)
                                    launchEnrollment()
                                }
                            )
                        })
                    }

                    is AppScreen.Locked -> {
                        LockedScreen(onUnlockRequest = {
                            biometricAuthManager.authenticateForDecryption(
                                iv = viewModel.getIvForDecryption(),
                                onSuccess = viewModel::onUnlockSuccess,
                                onFailure = viewModel::onUnlockFailure,
                                onEnrollmentRequired = { actionToRetry ->
                                    viewModel.setPendingBiometricAction(actionToRetry)
                                    launchEnrollment()
                                }
                            )
                        })
                    }

                    is AppScreen.RecoverPassword -> {
                        var error by remember { mutableStateOf<String?>(null) }
                        ManualPasswordScreen(
                            error = error,
                            onPasswordSubmit = { passwordAttempt ->
                                viewModel.onRecoveryPasswordSubmit(passwordAttempt) { isCorrect ->
                                    if (isCorrect) {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.password_verified_re_encrypt),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        biometricAuthManager.authenticateForEncryption(
                                            onSuccess = { cipher ->
                                                viewModel.onRecoverySuccess(
                                                    passwordAttempt,
                                                    cipher
                                                )
                                            },
                                            onEnrollmentRequired = { actionToRetry ->
                                                viewModel.setPendingBiometricAction(actionToRetry)
                                                launchEnrollment()
                                            }
                                        )
                                    } else {
                                        error = getString(R.string.incorrect_password_try_again)
                                    }
                                }
                            }
                        )
                    }

                    is AppScreen.Principal -> {
                        MainAppContainer(
                            hasNfcSupport = hasNfcSupport,
                            mainViewModel = viewModel,
                            masterPassword = currentScreen.masterPassword,
                            onAddImage = {
                                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onReauthenticate = { titleResId, subtitleResId, action ->
                                biometricAuthManager.authenticateForAction(
                                    titleResId = titleResId,
                                    subtitleResId = subtitleResId,
                                    onSuccess = action
                                )
                            },
                            onPurchaseRequest = { billingManager.launchPurchaseFlow(this) },
                            onEditEntry = { fileName -> viewModel.navigateToAddEntry(fileName) }
                        )
                        currentScreen.decryptedNfcSecret?.let { secret ->
                            AlertDialog(
                                onDismissRequest = viewModel::onDismissNfcSecretDialog,
                                title = { Text(stringResource(R.string.decrypted_secret)) },
                                text = { Text(secret) },
                                confirmButton = {
                                    TextButton(onClick = viewModel::onDismissNfcSecretDialog) {
                                        Text(stringResource(R.string.close))
                                    }
                                }
                            )
                        }
                    }

                    is AppScreen.AddEntry -> {
                        AddEntryScreen(
                            masterPassword = currentScreen.masterPassword,
                            onNavigateBack = viewModel::navigateToPrincipal,
                            fileNameToEdit = currentScreen.fileNameToEdit
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcHandler.setupNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcHandler.disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcHandler.handleIntent(intent)
    }

    private fun launchEnrollment() {
        Toast.makeText(this, getString(R.string.setup_lock_screen_prompt), Toast.LENGTH_LONG).show()

        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        enrollLauncher.launch(enrollIntent)
    }
}
