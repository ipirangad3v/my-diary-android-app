package digital.tonima.mydiary.biometrics

import javax.crypto.Cipher

interface BiometricAuthManager {
    fun authenticateForEncryption(
        onSuccess: (Cipher) -> Unit,
        onEnrollmentRequired: (actionToRetry: () -> Unit) -> Unit
    )

    fun authenticateForDecryption(
        iv: ByteArray,
        onSuccess: (Cipher) -> Unit,
        onFailure: () -> Unit,
        onEnrollmentRequired: (actionToRetry: () -> Unit) -> Unit
    )

    fun authenticateForAction(
        titleResId: Int,
        subtitleResId: Int,
        onSuccess: () -> Unit
    )
}
