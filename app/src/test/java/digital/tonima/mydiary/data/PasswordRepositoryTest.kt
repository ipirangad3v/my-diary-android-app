package digital.tonima.mydiary.data

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the [PasswordRepositoryImpl] class.
 * This test class uses Robolectric to provide a real Android Context
 * and an in-memory SharedPreferences implementation.
 */
@RunWith(RobolectricTestRunner::class) // Use AndroidJUnit4 runner for Robolectric
class PasswordRepositoryImplTest {

    // The class under test
    private lateinit var passwordRepository: PasswordRepository

    @Before
    fun setUp() {
        // Get a real context provided by Robolectric
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize the repository before each test to ensure a clean state
        passwordRepository = PasswordRepositoryImpl(context)
    }

    @Test
    fun `saveEncryptedPassword and getEncryptedPassword work correctly`() {
        // Arrange: Create a sample encrypted password object
        val originalPassword = EncryptedPassword(
            value = "my-secret-encrypted-data".toByteArray(),
            iv = "my-initialization-vector".toByteArray()
        )

        // Act: Save the password
        passwordRepository.saveEncryptedPassword(originalPassword)
        // Retrieve the password
        val retrievedPassword = passwordRepository.getEncryptedPassword()

        // Assert: The retrieved data should match the original data
        assertThat(retrievedPassword).isNotNull()
        // Truth's isEqualTo for byte arrays correctly checks for content equality
        assertThat(retrievedPassword!!.value).isEqualTo(originalPassword.value)
        assertThat(retrievedPassword.iv).isEqualTo(originalPassword.iv)
    }

    @Test
    fun `getEncryptedPassword returns null when no password is saved`() {
        // Arrange: No password has been saved yet

        // Act: Try to retrieve a password
        val retrievedPassword = passwordRepository.getEncryptedPassword()

        // Assert: The result should be null
        assertThat(retrievedPassword).isNull()
    }

    @Test
    fun `hasPassword returns false when no password is saved`() {
        // Arrange: No password has been saved yet

        // Act: Check for the existence of a password
        val result = passwordRepository.hasPassword()

        // Assert: The result should be false
        assertThat(result).isFalse()
    }

    @Test
    fun `hasPassword returns true after a password has been saved`() {
        // Arrange: Save a dummy password
        val dummyPassword = EncryptedPassword("value".toByteArray(), "iv".toByteArray())
        passwordRepository.saveEncryptedPassword(dummyPassword)

        // Act: Check for the existence of a password
        val result = passwordRepository.hasPassword()

        // Assert: The result should be true
        assertThat(result).isTrue()
    }

    @Test
    fun `getEncryptedPassword returns null if data is corrupted or not valid Base64`() {
        // Arrange: Manually insert invalid data into SharedPreferences
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("encrypted_password", "this is not valid base64!@#$")
            .putString("password_iv", Base64.encodeToString("valid_iv".toByteArray(), Base64.DEFAULT))
            .commit()

        // Act: Try to retrieve the corrupted password
        val retrievedPassword = passwordRepository.getEncryptedPassword()

        // Assert: The result should be null due to the decoding error
        assertThat(retrievedPassword).isNull()
    }
}