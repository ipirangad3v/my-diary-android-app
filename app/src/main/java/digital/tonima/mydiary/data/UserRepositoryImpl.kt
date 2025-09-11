package digital.tonima.mydiary.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.VIEW_MODEL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val PREF_KEY_IS_PRO = "is_pro_user"

@BindType(installIn = VIEW_MODEL, to = UserRepository::class)
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : UserRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object Companion {
        private const val PREFS_NAME = "diary_prefs"
    }


    override fun isProUser(): Boolean {
        return prefs.getBoolean(PREF_KEY_IS_PRO, false)
    }

    override fun setProUser(isPro: Boolean) {
        prefs.edit {
            putBoolean(PREF_KEY_IS_PRO, isPro)
        }
    }
}
