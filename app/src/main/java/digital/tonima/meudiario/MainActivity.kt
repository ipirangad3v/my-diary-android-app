package digital.tonima.meudiario

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.tonima.meudiario.ui.screens.AddEntryScreen
import digital.tonima.meudiario.ui.screens.LockedScreen
import digital.tonima.meudiario.ui.screens.MainScreen
import digital.tonima.meudiario.ui.theme.MeuDiarioTheme
import java.util.concurrent.Executor

sealed class Screen {
    object Main : Screen()
    object AddEntry : Screen()
}

class MainActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val isAuthenticated = mutableStateOf(false)

    private val enrollLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkDeviceCapabilityAndAuthenticate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        setContent {
            MeuDiarioTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                val isAuthenticatedValue by isAuthenticated

                if (isAuthenticatedValue) {
                    when (currentScreen) {
                        is Screen.Main -> MainScreen(
                            onLockRequest = { isAuthenticated.value = false },
                            onNavigateToAddEntry = { currentScreen = Screen.AddEntry }
                        )
                        is Screen.AddEntry -> AddEntryScreen(
                            onNavigateBack = {
                                currentScreen = Screen.Main
                            }
                        )
                    }
                } else {
                    LockedScreen(onUnlockRequest = {
                        checkDeviceCapabilityAndAuthenticate()
                    })
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated.value = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (!(errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED)) {
                    Toast.makeText(applicationContext, getString(R.string.autentication_error, errString), Toast.LENGTH_SHORT).show()
                }
            }
        }

        biometricPrompt = BiometricPrompt(this, executor, authenticationCallback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.acess_to_diary))
            .setSubtitle(getString(R.string.use_pin_or_digital_to_continue))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated.value) {
            checkDeviceCapabilityAndAuthenticate()
        }
    }

    private fun checkDeviceCapabilityAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, getString(R.string.none_pin_or_digital_set), Toast.LENGTH_LONG).show()
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                    )
                }
                enrollLauncher.launch(enrollIntent)
            }
            else -> {
                Toast.makeText(this, getString(R.string.unsupported_auth), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

