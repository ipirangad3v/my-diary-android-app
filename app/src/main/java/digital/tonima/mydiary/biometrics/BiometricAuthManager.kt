package digital.tonima.mydiary.biometrics

import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.tonima.mydiary.R
import digital.tonima.mydiary.data.KeystoreCryptoManager
import java.security.InvalidAlgorithmParameterException
import javax.crypto.Cipher

/**
 * Manages all interactions with the BiometricPrompt API.
 * This class abstracts away the details of creating and handling biometric authentication.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    private val executor = ContextCompat.getMainExecutor(activity)
    private val allowedAuthenticators = DEVICE_CREDENTIAL or BIOMETRIC_STRONG


    /**
     * Checks device capability and then shows a biometric prompt for an encryption operation.
     * On success, provides a ready-to-use encryption Cipher.
     * If no screen lock is set up, it triggers the onEnrollmentRequired callback.
     */
    fun authenticateForEncryption(onSuccess: (Cipher) -> Unit, onEnrollmentRequired: () -> Unit) {
        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BIOMETRIC_SUCCESS -> {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.secure_your_password))
                    .setSubtitle(activity.getString(R.string.confirm_to_encrypt))
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build()
                try {
                    val encryptCipher = KeystoreCryptoManager.getEncryptCipher()
                    val biometricPrompt = createBiometricPrompt(
                        onSuccess = { result -> result.cryptoObject?.cipher?.let(onSuccess) }
                    )
                    biometricPrompt.authenticate(
                        promptInfo,
                        BiometricPrompt.CryptoObject(encryptCipher)
                    )
                } catch (e: InvalidAlgorithmParameterException) {
                    // This exception indicates that the key is invalidated.
                    // This can happen if the user has removed or added a new biometric credential.
                    // In this case, we should prompt the user to re-enroll.
                    Log.e("BiometricAuthManager", "Key invalidated, re-enrollment required", e)
                    onEnrollmentRequired()
                } catch (e: Exception) {
                    Log.e("BiometricAuthManager", "Error setting up encryption", e)
                }
            }

            BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // The user can create credentials in settings.
                onEnrollmentRequired()
            }

            else -> {
                // Other errors like no hardware, etc.
                Toast.makeText(
                    activity,
                    "Authentication is not supported on this device",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Checks device capability and then shows a biometric prompt for a decryption operation.
     * On success, provides a ready-to-use decryption Cipher.
     * On failure (e.g., key not found on a new device), invokes the onFailure callback.
     * If no screen lock is set up, it triggers the onEnrollmentRequired callback.
     */
    fun authenticateForDecryption(
        iv: ByteArray,
        onSuccess: (Cipher) -> Unit,
        onFailure: () -> Unit,
        onEnrollmentRequired: () -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BIOMETRIC_SUCCESS -> {
                // Device is ready for authentication.
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.acess_to_diary))
                    .setSubtitle(activity.getString(R.string.use_pin_or_digital_to_continue))
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build()
                try {
                    val decryptCipher = KeystoreCryptoManager.getDecryptCipherForIv(iv)
                    val biometricPrompt = createBiometricPrompt(
                        onSuccess = { result ->
                            result.cryptoObject?.cipher?.let(onSuccess) ?: onFailure()
                        },
                        onError = { onFailure() }
                    )
                    biometricPrompt.authenticate(
                        promptInfo,
                        BiometricPrompt.CryptoObject(decryptCipher)
                    )
                } catch (e: Exception) {
                    Log.e("BiometricAuthManager", "Error setting up decryption", e)
                    onFailure()
                }
            }

            BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // The user can create credentials in settings.
                onEnrollmentRequired()
            }

            else -> {
                // Other errors like no hardware, etc.
                Toast.makeText(
                    activity,
                    "Authentication is not supported on this device",
                    Toast.LENGTH_LONG
                ).show()
                onFailure()
            }
        }
    }

    /**
     * Creates a reusable BiometricPrompt instance with generalized callbacks.
     */
    private fun createBiometricPrompt(
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (() -> Unit)? = null
    ): BiometricPrompt {
        return BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Log.e(
                            "BiometricAuthManager",
                            "Authentication error ($errorCode): $errString"
                        )
                        onError?.invoke()
                    }
                }
            })
    }

    fun authenticateForAction(
        titleResId: Int,
        subtitleResId: Int,
        onSuccess: () -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(titleResId))
            .setSubtitle(activity.getString(subtitleResId))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = createBiometricPrompt(
            onSuccess = { onSuccess() }
        )
        biometricPrompt.authenticate(promptInfo)
    }
}
