package digital.tonima.mydiary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mydiary.biometrics.BiometricAuthManager
import digital.tonima.mydiary.ui.screens.*
import digital.tonima.mydiary.ui.theme.MyDiaryTheme
import digital.tonima.mydiary.R.string.incorrect_password_try_again
import digital.tonima.mydiary.R.string.password_verified_re_encrypt

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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
                            biometricAuthManager.authenticateForEncryption { cipher ->
                                viewModel.onPasswordSetup(password, cipher)
                            }
                        }
                    }
                    is AppScreen.Locked -> {
                        LockedScreen(onUnlockRequest = {
                            val iv = viewModel.getIvForDecryption()
                            biometricAuthManager.authenticateForDecryption(
                                iv = iv,
                                onSuccess = viewModel::onUnlockSuccess,
                                onFailure = viewModel::onUnlockFailure
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
                                        Toast.makeText(this, getString(password_verified_re_encrypt), Toast.LENGTH_LONG).show()
                                        biometricAuthManager.authenticateForEncryption { cipher ->
                                            viewModel.onRecoverySuccess(passwordAttempt, cipher)
                                        }
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
}

