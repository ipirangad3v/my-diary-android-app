package digital.tonima.mydiary

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mydiary.biometrics.BiometricAuthManager
import digital.tonima.mydiary.ui.screens.*
import digital.tonima.mydiary.ui.theme.MyDiaryTheme
import digital.tonima.mydiary.R.string.incorrect_password_try_again
import digital.tonima.mydiary.R.string.password_verified_re_encrypt
import digital.tonima.mydiary.R.string.setup_lock_screen_prompt

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val enrollLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        viewModel.retryBiometricAction()
    }


    private lateinit var biometricAuthManager: BiometricAuthManager
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            MyDiaryTheme {
                val screen by viewModel.uiState.collectAsStateWithLifecycle()

                when (val currentScreen = screen) {
                    is AppScreen.SetupPassword -> {
                        PasswordSetupScreen { password ->
                            biometricAuthManager.authenticateForEncryption(
                                onSuccess = { cipher ->
                                    viewModel.onPasswordSetup(password, cipher)
                                },
                                onEnrollmentRequired = {
                                    launchEnrollment()
                                })
                        }
                    }

                    is AppScreen.Locked -> {
                        LockedScreen(onUnlockRequest = {
                            val iv = viewModel.getIvForDecryption()
                            biometricAuthManager.authenticateForDecryption(
                                iv = iv,
                                onSuccess = viewModel::onUnlockSuccess,
                                onFailure = viewModel::onUnlockFailure,
                                onEnrollmentRequired = {
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
                                        makeText(
                                            this, getString(password_verified_re_encrypt),
                                            LENGTH_LONG
                                        ).show()
                                        biometricAuthManager.authenticateForEncryption(
                                            onSuccess = { cipher ->
                                                viewModel.onRecoverySuccess(passwordAttempt, cipher)
                                            },
                                            onEnrollmentRequired = {
                                                launchEnrollment()
                                            }
                                        )
                                    } else {
                                        error = getString(incorrect_password_try_again)
                                    }
                                }
                            }
                        )
                    }

                    is AppScreen.Main -> {
                        MainScreen(
                            masterPassword = currentScreen.masterPassword,
                            onLockRequest = viewModel::lockApp,
                            onAddEntry = viewModel::navigateToAddEntry
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
        makeText(this, getString(setup_lock_screen_prompt), LENGTH_LONG).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Directly open biometric enrollment settings on Android 11+
            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )
            }
            startActivity(enrollIntent)
        } else {
            // Fallback to general security settings on older versions
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            startActivity(intent)
        }
        val enrollIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        enrollLauncher.launch(enrollIntent)
    }
}
