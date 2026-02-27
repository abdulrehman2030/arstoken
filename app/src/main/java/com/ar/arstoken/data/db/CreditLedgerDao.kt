package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CreditLedgerDao {

    @Query("DELETE FROM credit_ledger")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(entry: CreditLedgerEntity)

    @Query("SELECT * FROM credit_ledger")
    suspend fun getAllOnce(): List<CreditLedgerEntity>

    @Query("SELECT * FROM credit_ledger WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): CreditLedgerEntity?

    @Query("""
        UPDATE credit_ledger
        SET customerId = :customerId,
            customerCloudId = :customerCloudId,
            customerName = :customerName,
            saleId = :saleId,
            saleCloudId = :saleCloudId,
            timestamp = :timestamp,
            totalAmount = :totalAmount,
            paidAmount = :paidAmount,
            dueAmount = :dueAmount,
            synced = :synced,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        customerId: Int,
        customerCloudId: String?,
        customerName: String,
        saleId: String,
        saleCloudId: String?,
        timestamp: Long,
        totalAmount: Double,
        paidAmount: Double,
        dueAmount: Double,
        synced: Boolean,
        updatedAt: Long
    )

    @Query("""
        UPDATE credit_ledger
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)

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
