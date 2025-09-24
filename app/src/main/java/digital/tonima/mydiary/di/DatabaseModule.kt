package digital.tonima.mydiary.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.mydiary.database.AppDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fornece uma instância da base de dados Room encriptada.
 * A chave de encriptação é derivada da senha mestra do utilizador e é fornecida em tempo de execução.
 */
@Singleton
class DatabaseProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context
    ) {
        private var database: AppDatabase? = null

        fun getDatabase(passphrase: CharArray): AppDatabase {
            if (database?.isOpen == true) {
                return database!!
            }

            val factory = SupportFactory(passphrase.joinToString("").toByteArray())

            return Room.databaseBuilder(context, AppDatabase::class.java, "diary.db")
                .openHelperFactory(factory)
                .build()
                .also {
                    database = it
                }
        }

        fun closeDatabase() {
            if (database?.isOpen == true) {
                database?.close()
            }
            database = null
        }
    }

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(
        @ApplicationContext context: Context
    ): DatabaseProvider {
        return DatabaseProvider(context)
    }
}
