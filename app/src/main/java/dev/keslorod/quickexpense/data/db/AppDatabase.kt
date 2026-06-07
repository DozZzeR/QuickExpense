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
import dev.keslorod.quickexpense.data.dao.SplitNodeDao
import dev.keslorod.quickexpense.data.dao.SplitNodeTagDao
import dev.keslorod.quickexpense.data.dao.TagDao
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.Merchant
import dev.keslorod.quickexpense.data.entities.Source
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.SplitNodeTag
import dev.keslorod.quickexpense.data.entities.Tag

@Database(
    entities = [
        Expense::class,
        Category::class,
        Source::class,
        Merchant::class,
        Tag::class,
        SplitNode::class,
        SplitNodeTag::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun categories(): CategoryDao
    abstract fun sources(): SourceDao
    abstract fun merchants(): MerchantDao
    abstract fun tags(): TagDao
    abstract fun splitNodes(): SplitNodeDao
    abstract fun splitNodeTags(): SplitNodeTagDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_nodes` (`id` TEXT NOT NULL, `expenseId` TEXT NOT NULL, `parentId` TEXT, `amount` INTEGER NOT NULL, `label` TEXT, `categoryId` TEXT, `depth` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`parentId`) REFERENCES `split_nodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_node_tags` (`splitNodeId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`splitNodeId`, `tagId`), FOREIGN KEY(`splitNodeId`) REFERENCES `split_nodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_nodes_expenseId` ON `split_nodes` (`expenseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_nodes_parentId` ON `split_nodes` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_nodes_categoryId` ON `split_nodes` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_node_tags_splitNodeId` ON `split_node_tags` (`splitNodeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_node_tags_tagId` ON `split_node_tags` (`tagId`)")

        // Add normalizedName to merchants and categories
        db.execSQL("ALTER TABLE merchants ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE merchants SET normalizedName = LOWER(TRIM(name))")
        
        db.execSQL("ALTER TABLE categories ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE categories SET normalizedName = LOWER(TRIM(name))")
    }
}
