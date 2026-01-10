package com.ar.arstoken.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SaleEntity::class,
        SaleItemEntity::class,
        CustomerEntity::class,
        CreditLedgerEntity::class,
        ItemEntity::class
    ],
    version = 2   // 👈 increment version
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun saleDao(): SaleDao
    abstract fun creditLedgerDao(): CreditLedgerDao
    abstract fun customerDao(): CustomerDao
    abstract fun itemDao(): ItemDao
    abstract fun saleItemDao(): SaleItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arstoken.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
