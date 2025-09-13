package digital.tonima.mydiary

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_BIOMETRIC_ENROLL
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mydiary.R.string.setup_lock_screen_prompt
import digital.tonima.mydiary.billing.BillingManager
import digital.tonima.mydiary.biometrics.BiometricAuthManager
import digital.tonima.mydiary.biometrics.BiometricAuthManagerImpl
import digital.tonima.mydiary.ui.screens.AddEntryScreen
import digital.tonima.mydiary.ui.screens.AppScreen
import digital.tonima.mydiary.ui.screens.LockedScreen
import digital.tonima.mydiary.ui.screens.MainAppContainer
import digital.tonima.mydiary.ui.screens.ManualPasswordScreen
import digital.tonima.mydiary.ui.screens.PasswordSetupScreen
import digital.tonima.mydiary.ui.theme.MyDiaryTheme
import digital.tonima.mydiary.ui.viewmodels.VaultViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var billingManager: BillingManager
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager


    private val viewModel: MainViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val enrollLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from settings, retry the stored biometric action.
        viewModel.retryBiometricAction()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager.connect()
        enableEdgeToEdge()
        biometricAuthManager = BiometricAuthManagerImpl(this)

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                (viewModel.uiState.value as? AppScreen.Main)?.masterPassword?.let { password ->
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
                                onSuccess = { cipher ->
                                    viewModel.onPasswordSetup(password, cipher)
                                },
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
                                            this, getString(R.string.password_verified_re_encrypt),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        biometricAuthManager.authenticateForEncryption(
                                            onSuccess = { cipher ->
                                                viewModel.onRecoverySuccess(passwordAttempt, cipher)
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

                    is AppScreen.Main -> {
                        MainAppContainer(
                            mainViewModel = viewModel,
                            masterPassword = currentScreen.masterPassword,
                            onAddImage = {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onReauthenticate = { titleResId, subtitleResId, action ->
                                biometricAuthManager.authenticateForAction(
                                    titleResId = titleResId,
                                    subtitleResId = subtitleResId,
                                    onSuccess = action
                                )
                            },
                            onPurchaseRequest = { billingManager.launchPurchaseFlow(this) }
                        )
                    }

                    is AppScreen.AddEntry -> {
                        AddEntryScreen(
                            masterPassword = currentScreen.masterPassword,
                            onNavigateBack = viewModel::navigateToMain
                        )
                    }
                }
            }
        }
    }

    private fun launchEnrollment() {
        Toast.makeText(this, getString(setup_lock_screen_prompt), Toast.LENGTH_LONG).show()

        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(ACTION_SECURITY_SETTINGS)
        }
        enrollLauncher.launch(enrollIntent)
    }
}
