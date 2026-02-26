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

    // Update running credit balance
    @Query("""
        UPDATE customers
        SET creditBalance = creditBalance + :amount
        WHERE id = :customerId
    """)
    suspend fun updateBalance(
        customerId: Int,
        amount: Int
    )

    // Get single customer's balance
    @Query("""
        SELECT creditBalance
        FROM customers
        WHERE id = :customerId
    """)
    suspend fun getBalance(customerId: Int): Int?

    @Query("""
    UPDATE customers
    SET phone = :phone
    WHERE id = :customerId
""")
    suspend fun updatePhone(
        customerId: Int,
        phone: String
    )

}
