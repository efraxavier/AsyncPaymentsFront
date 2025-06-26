package com.example.asyncpayments.data

import androidx.room.*
import com.example.asyncpayments.data.OfflineTransactionEntity

@Dao
interface OfflineTransactionDao {
    @Query("SELECT * FROM offline_transactions")
    suspend fun getAll(): List<OfflineTransactionEntity>

    @Update
    suspend fun update(entity: OfflineTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineTransactionEntity)

}