package com.ar.arstoken.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SaleEntity::class,
        SaleItemEntity::class,
        CustomerEntity::class,
        CreditLedgerEntity::class,
        ItemEntity::class,
        StoreSettingsEntity::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun saleDao(): SaleDao
    abstract fun creditLedgerDao(): CreditLedgerDao
    abstract fun customerDao(): CustomerDao
    abstract fun itemDao(): ItemDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun storeSettingsDao(): StoreSettingsDao


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `items_new` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `items_new` (`id`, `name`, `price`, `isActive`)
                    SELECT `id`, `name`, `price`, `isActive` FROM `items`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `items`")
                db.execSQL("ALTER TABLE `items_new` RENAME TO `items`")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arstoken.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
