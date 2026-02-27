package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // Insert new customer
    @Insert
    suspend fun insert(customer: CustomerEntity)

    // Get all customers (for admin & dropdown)
    @Query("SELECT * FROM customers ORDER BY name")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAllOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): CustomerEntity?

    @Query("""
        UPDATE customers
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)

    @Query("""
        UPDATE customers
        SET name = :name,
            phone = :phone,
            creditBalance = :creditBalance,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        name: String,
        phone: String,
        creditBalance: Double,
        updatedAt: Long
    )

    // Update running credit balance
    @Query("""
        UPDATE customers
        SET creditBalance = creditBalance + :amount,
            updatedAt = :updatedAt
        WHERE id = :customerId
    """)
    suspend fun updateBalance(
        customerId: Int,
        amount: Double,
        updatedAt: Long
    )

    // Get single customer's balance
    @Query("""
        SELECT creditBalance
        FROM customers
        WHERE id = :customerId
    """)
    suspend fun getBalance(customerId: Int): Double?

    @Query("""
    UPDATE customers
    SET phone = :phone,
        updatedAt = :updatedAt
    WHERE id = :customerId
""")
    suspend fun updatePhone(
        customerId: Int,
        phone: String,
        updatedAt: Long
    )

}
