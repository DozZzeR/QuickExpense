package dev.keslorod.quickexpense.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.keslorod.quickexpense.data.dao.CategoryDao
import dev.keslorod.quickexpense.data.dao.ExpenseDao
import dev.keslorod.quickexpense.data.dao.MerchantDao
import dev.keslorod.quickexpense.data.dao.SourceDao
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.Merchant
import dev.keslorod.quickexpense.data.entities.Source

@Database(
    entities = [Expense::class, Category::class, Source::class, Merchant::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun categories(): CategoryDao
    abstract fun sources(): SourceDao
    abstract fun merchants(): MerchantDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickexpense.db"
                )
                    // .fallbackToDestructiveMigration() // не включаем, будем писать миграции
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchants (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                sort INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE expenses ADD COLUMN merchantId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_merchantId ON expenses(merchantId)")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN photoPaths TEXT")
    }
}
