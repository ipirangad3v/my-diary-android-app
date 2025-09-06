package digital.tonima.mydiary.biometrics

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.tonima.mydiary.R
import digital.tonima.mydiary.data.KeystoreCryptoManager
import javax.crypto.Cipher

/**
 * Manages all interactions with the BiometricPrompt API.
 * This class abstracts away the details of creating and handling biometric authentication.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    private val executor = ContextCompat.getMainExecutor(activity)

    /**
     * Shows a biometric prompt to the user for an encryption operation.
     * On success, provides a ready-to-use encryption Cipher.
     */
    fun authenticateForEncryption(onSuccess: (Cipher) -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.secure_your_password))
            .setSubtitle(activity.getString(R.string.confirm_to_encrypt))
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .build()

        try {
            val encryptCipher = KeystoreCryptoManager.getEncryptCipher()
            val biometricPrompt = createBiometricPrompt(
                onSuccess = { result -> result.cryptoObject?.cipher?.let(onSuccess) }
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(encryptCipher))
        } catch (e: Exception) {
            Toast.makeText(activity, "Error setting up encryption: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a biometric prompt to the user for a decryption operation.
     * On success, provides a ready-to-use decryption Cipher.
     * On failure (e.g., key not found on a new device), invokes the onFailure callback.
     */
    fun authenticateForDecryption(iv: ByteArray, onSuccess: (Cipher) -> Unit, onFailure: () -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.acess_to_diary))
            .setSubtitle(activity.getString(R.string.use_pin_or_digital_to_continue))
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .build()

        try {
            val decryptCipher = KeystoreCryptoManager.getDecryptCipherForIv(iv)
            val biometricPrompt = createBiometricPrompt(
                onSuccess = { result ->
                    result.cryptoObject?.cipher?.let(onSuccess)
                        ?: onFailure() // Cipher is null, treat as failure
                },
                onError = {
                    // This is the expected failure on a new device.
                    onFailure()
                }
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(decryptCipher))
        } catch (e: Exception) {
            // Failure to even initialize the cipher means the key is gone (new device).
            onFailure()
        }
    }

    /**
     * Creates a reusable BiometricPrompt instance with generalized callbacks.
     */
    private fun createBiometricPrompt(
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (() -> Unit)? = null
    ): BiometricPrompt {
        return BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled is not an error that should trigger the failure path.
                    // Any other error (e.g. lockout) should trigger the failure path.
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onError?.invoke()
                    }
                }
            })
    }
}
