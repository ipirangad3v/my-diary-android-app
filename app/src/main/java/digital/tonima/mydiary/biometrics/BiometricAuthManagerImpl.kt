package digital.tonima.mydiary.biometrics

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.ACTIVITY
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import digital.tonima.mydiary.R.string.acess_to_diary
import digital.tonima.mydiary.R.string.confirm_to_encrypt
import digital.tonima.mydiary.R.string.secure_your_password
import digital.tonima.mydiary.R.string.use_pin_or_digital_to_continue
import digital.tonima.mydiary.encrypting.KeystoreCryptoManager
import javax.crypto.Cipher
import javax.inject.Inject

@BindType(installIn = ACTIVITY, to = BiometricAuthManager::class)
@ActivityScoped
class BiometricAuthManagerImpl
    @Inject
    constructor(
        @ActivityContext private val context: Context
    ) : BiometricAuthManager {
        private val activity = context as FragmentActivity
        private val executor = ContextCompat.getMainExecutor(context)

        override fun authenticateForEncryption(
            onSuccess: (Cipher) -> Unit,
            onEnrollmentRequired: (actionToRetry: () -> Unit) -> Unit
        ) {
            val action = { authenticateForEncryption(onSuccess, onEnrollmentRequired) }
            if (isEnrollmentRequired(action, onEnrollmentRequired)) return

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(secure_your_password))
                .setSubtitle(activity.getString(confirm_to_encrypt))
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            try {
                val encryptCipher = KeystoreCryptoManager.getEncryptCipher()
                val biometricPrompt = createBiometricPrompt(
                    onSuccess = { result -> result.cryptoObject?.cipher?.let(onSuccess) }
                )
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(encryptCipher))
            } catch (e: Exception) {
                Log.e("BiometricAuthManager", "Error setting up encryption", e)
            }
        }

        override fun authenticateForDecryption(
            iv: ByteArray,
            onSuccess: (Cipher) -> Unit,
            onFailure: () -> Unit,
            onEnrollmentRequired: (actionToRetry: () -> Unit) -> Unit
        ) {
            val action = { authenticateForDecryption(iv, onSuccess, onFailure, onEnrollmentRequired) }
            if (isEnrollmentRequired(action, onEnrollmentRequired)) return

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(acess_to_diary))
                .setSubtitle(activity.getString(use_pin_or_digital_to_continue))
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            try {
                val decryptCipher = KeystoreCryptoManager.getDecryptCipherForIv(iv)
                val biometricPrompt = createBiometricPrompt(
                    onSuccess = { result ->
                        result.cryptoObject?.cipher?.let(onSuccess) ?: onFailure()
                    },
                    onError = onFailure
                )
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(decryptCipher))
            } catch (e: Exception) {
                Log.e("BiometricAuthManager", "Error setting up decryption", e)
                onFailure()
            }
        }

        private fun isEnrollmentRequired(
            actionToRetry: () -> Unit,
            onEnrollmentRequired: (actionToRetry: () -> Unit) -> Unit
        ): Boolean {
            val biometricManager = BiometricManager.from(context)
            val authenticators =
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            if (biometricManager.canAuthenticate(authenticators) == BIOMETRIC_ERROR_NONE_ENROLLED) {
                onEnrollmentRequired(actionToRetry)
                return true
            }
            return false
        }

        override fun authenticateForAction(
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

        private fun createBiometricPrompt(
            onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
            onError: (() -> Unit)? = null
        ): BiometricPrompt {
            return BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess(result)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != ERROR_NEGATIVE_BUTTON && errorCode != ERROR_USER_CANCELED) {
                            onError?.invoke()
                        }
                    }
                }
            )
        }
    }
