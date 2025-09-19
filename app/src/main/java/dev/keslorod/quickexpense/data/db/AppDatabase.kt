package dev.keslorod.quickexpense.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.keslorod.quickexpense.data.dao.CategoryDao
import dev.keslorod.quickexpense.data.dao.ExpenseDao
import dev.keslorod.quickexpense.data.dao.SourceDao
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.Source

@Database(
    entities = [Expense::class, Category::class, Source::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun categories(): CategoryDao
    abstract fun sources(): SourceDao

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
                    .build().also { INSTANCE = it }
            }
    }
}
