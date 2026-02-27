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
        CategoryEntity::class,
        StoreSettingsEntity::class,
        BusinessProfileEntity::class
    ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun saleDao(): SaleDao
    abstract fun creditLedgerDao(): CreditLedgerDao
    abstract fun customerDao(): CustomerDao
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun businessProfileDao(): BusinessProfileDao


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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE items
                    ADD COLUMN category TEXT
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // items
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `items_new` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `category` TEXT,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `items_new` (`id`, `name`, `price`, `category`, `isActive`)
                    SELECT `id`, `name`, CAST(`price` AS INTEGER), `category`, `isActive`
                    FROM `items`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `items`")
                db.execSQL("ALTER TABLE `items_new` RENAME TO `items`")

                // customers
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `customers_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `creditBalance` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `customers_new` (`id`, `name`, `phone`, `creditBalance`)
                    SELECT `id`, `name`, `phone`, CAST(`creditBalance` AS INTEGER)
                    FROM `customers`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `customers`")
                db.execSQL("ALTER TABLE `customers_new` RENAME TO `customers`")

                // sales
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sales_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `customerId` INTEGER,
                        `customerName` TEXT,
                        `saleType` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `dueAmount` INTEGER NOT NULL,
                        `synced` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sales_new` (`id`, `timestamp`, `customerId`, `customerName`, `saleType`, `totalAmount`, `paidAmount`, `dueAmount`, `synced`)
                    SELECT `id`, `timestamp`, `customerId`, `customerName`, `saleType`,
                           CAST(`totalAmount` AS INTEGER), CAST(`paidAmount` AS INTEGER), CAST(`dueAmount` AS INTEGER), `synced`
                    FROM `sales`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `sales`")
                db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")

                // sale_items
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sale_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `itemName` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `totalPrice` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sale_items_new` (`id`, `saleId`, `itemId`, `itemName`, `quantity`, `unitPrice`, `totalPrice`, `timestamp`)
                    SELECT `id`, `saleId`, `itemId`, `itemName`,
                           CAST(`quantity` AS INTEGER), CAST(`unitPrice` AS INTEGER), CAST(`totalPrice` AS INTEGER), `timestamp`
                    FROM `sale_items`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `sale_items`")
                db.execSQL("ALTER TABLE `sale_items_new` RENAME TO `sale_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")

                // credit_ledger
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_ledger_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `saleId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `dueAmount` INTEGER NOT NULL,
                        `synced` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `credit_ledger_new` (`id`, `customerId`, `customerName`, `saleId`, `timestamp`, `totalAmount`, `paidAmount`, `dueAmount`, `synced`)
                    SELECT `id`, `customerId`, `customerName`, `saleId`, `timestamp`,
                           CAST(`totalAmount` AS INTEGER), CAST(`paidAmount` AS INTEGER), CAST(`dueAmount` AS INTEGER), `synced`
                    FROM `credit_ledger`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `credit_ledger`")
                db.execSQL("ALTER TABLE `credit_ledger_new` RENAME TO `credit_ledger`")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN primaryPrinterEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printerType TEXT NOT NULL DEFAULT 'BLUETOOTH'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN autoPrintSale INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN charactersPerLine INTEGER NOT NULL DEFAULT 32")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN column2Chars INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN column3Chars INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN column4Chars INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printTokenNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN businessNameSize TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN totalAmountSize TEXT NOT NULL DEFAULT 'SMALL'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printCreatedInfo INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printFooter TEXT NOT NULL DEFAULT 'Thank You! Visit Again!'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printerSpacingFix INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN bottomPaddingLines INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN printItemMultiLine INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN itemOrder TEXT NOT NULL DEFAULT 'INSERTED'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `business_profile` (
                        `id` INTEGER NOT NULL,
                        `businessName` TEXT NOT NULL,
                        `logoUrl` TEXT,
                        `phone` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // customers
                db.execSQL("ALTER TABLE customers ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE customers SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // items
                db.execSQL("ALTER TABLE items ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE items SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // categories
                db.execSQL("ALTER TABLE categories ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE categories SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // sales
                db.execSQL("ALTER TABLE sales ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN customerCloudId TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sales SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // sale_items
                db.execSQL("ALTER TABLE sale_items ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN saleCloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN itemCloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sale_items SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // credit_ledger
                db.execSQL("ALTER TABLE credit_ledger ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE credit_ledger ADD COLUMN customerCloudId TEXT")
                db.execSQL("ALTER TABLE credit_ledger ADD COLUMN saleCloudId TEXT")
                db.execSQL("ALTER TABLE credit_ledger ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE credit_ledger SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")

                // store_settings
                db.execSQL("ALTER TABLE store_settings ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE store_settings SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN syncEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN syncHour INTEGER NOT NULL DEFAULT 22")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN syncMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arstoken.db"
                )
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
