package digital.tonima.mydiary

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import digital.tonima.mydiary.delegates.ProUserProvider
import digital.tonima.mydiary.encrypting.EncryptedPassword
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import digital.tonima.mydiary.encrypting.PasswordRepository
import digital.tonima.mydiary.ui.screens.AppScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class MainViewModelTest {

    // Rule to execute LiveData/StateFlow tasks synchronously
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Rule to handle Coroutines in tests
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    // Mocks
    private lateinit var passwordRepository: PasswordRepository
    private lateinit var applicationContext: Context
    private lateinit var cipher: Cipher

    private lateinit var realContext: Context

    private lateinit var viewModel: MainViewModel

    private lateinit var proUserProvider: ProUserProvider

    private lateinit var cryptoManager: PasswordBasedCryptoManager

    @Before
    fun setUp() {
        // Initializes mocks before each test
        // `relaxed = true` allows the mock to return default values for non-stubbed calls
        passwordRepository = mockk(relaxed = true)
        applicationContext = mockk()
        cipher = mockk(relaxed = true)
        realContext = ApplicationProvider.getApplicationContext()
        proUserProvider = mockk(relaxed = true)
        cryptoManager = mockk(relaxed = true)
    }

    @Test
    fun `initial state is SetupPassword when no password exists`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns false

        // Act
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        // Assert
        assertThat(viewModel.uiState.value).isEqualTo(AppScreen.SetupPassword)
    }

    @Test
    fun `initial state is Locked when password exists`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns true

        // Act
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        // Assert
        assertThat(viewModel.uiState.value).isEqualTo(AppScreen.Locked)
    }

    @Test
    fun `onPasswordSetup saves encrypted password and navigates to MainScreen`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns false
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        val password = "test_password".toCharArray()
        val passwordBytes = password.concatToString().toByteArray()
        val encryptedBytes = "encrypted_data".toByteArray()
        val iv = "test_iv".toByteArray()

        every { cipher.doFinal(passwordBytes) } returns encryptedBytes
        every { cipher.iv } returns iv

        // Act & Assert
        viewModel.uiState.test {
            // Initial state
            assertThat(awaitItem()).isEqualTo(AppScreen.SetupPassword)

            viewModel.onPasswordSetup(password, cipher)

            // New state should be Main
            val newState = awaitItem() as AppScreen.Principal
            assertThat(newState.masterPassword).isEqualTo(password)

            // Verify that the password was saved correctly using a slot to capture the argument
            val captor = slot<EncryptedPassword>()
            verify { passwordRepository.saveEncryptedPassword(capture(captor)) }
            assertThat(captor.captured.value).isEqualTo(encryptedBytes)
            assertThat(captor.captured.iv).isEqualTo(iv)
        }
    }

    @Test
    fun `onUnlockSuccess decrypts password and navigates to MainScreen`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns true
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        val decryptedPassword = "my_secret_password"
        val encryptedBytes = "encrypted_data".toByteArray()
        val iv = "test_iv".toByteArray()
        val encryptedPassword = EncryptedPassword(encryptedBytes, iv)

        every { passwordRepository.getEncryptedPassword() } returns encryptedPassword
        every { cipher.doFinal(encryptedBytes) } returns decryptedPassword.toByteArray()

        // Act & Assert
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppScreen.Locked)
            viewModel.onUnlockSuccess(cipher)

            val newState = awaitItem() as AppScreen.Principal
            assertThat(String(newState.masterPassword)).isEqualTo(decryptedPassword)
        }
    }

    @Test
    fun `onUnlockSuccess navigates to RecoverPassword if no password is saved`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns true
        every { passwordRepository.getEncryptedPassword() } returns null
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        // Act & Assert
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppScreen.Locked)
            viewModel.onUnlockSuccess(cipher)
            assertThat(awaitItem()).isEqualTo(AppScreen.RecoverPassword)
        }
    }

    @Test
    fun `onUnlockFailure navigates to RecoverPassword`() = runTest {
        // Arrange
        every { passwordRepository.hasPassword() } returns true
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)

        // Act
        viewModel.onUnlockFailure()

        // Assert
        assertThat(viewModel.uiState.value).isEqualTo(AppScreen.RecoverPassword)
    }

    @Test
    fun `lockApp changes state to Locked`() = runTest {
        // Arrange: Start in a non-locked state
        every { passwordRepository.hasPassword() } returns false
        viewModel = MainViewModel(passwordRepository, cryptoManager, proUserProvider)
        // Put the ViewModel into the Main state
        every { cipher.doFinal(any()) } returns "encrypted".toByteArray()
        every { cipher.iv } returns "iv".toByteArray()
        viewModel.onPasswordSetup("password".toCharArray(), cipher)

        viewModel.uiState.test {
            // Initial states
            awaitItem() // SetupPassword
            assertThat(awaitItem()).isInstanceOf(AppScreen.Principal::class.java)

            // Act
            viewModel.lockApp()

            // Assert
            assertThat(awaitItem()).isEqualTo(AppScreen.Locked)
        }
    }
}
