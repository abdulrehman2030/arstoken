package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CreditLedgerDao {

    @Insert
    suspend fun insert(entry: CreditLedgerEntity)

    @Query("""
        SELECT SUM(dueAmount)
        FROM credit_ledger
        WHERE customerId = :customerId
    """)
    suspend fun getTotalDue(customerId: Int): Double?

    @Query("""
    SELECT SUM(dueAmount)
    FROM credit_ledger
""")
    suspend fun getOverallDue(): Double?

}

