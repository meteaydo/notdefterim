package com.notdefterim.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notdefterim.app.data.local.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
  @Query("SELECT * FROM passwords ORDER BY usageCount DESC, createdAt DESC")
  fun getAllPasswords(): Flow<List<PasswordEntity>>

  @Query("SELECT * FROM passwords WHERE platformName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY usageCount DESC, createdAt DESC")
  fun searchPasswords(query: String): Flow<List<PasswordEntity>>

  @Query("SELECT * FROM passwords WHERE id = :id")
  suspend fun getPasswordById(id: Long): PasswordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPassword(password: PasswordEntity): Long

  @Update
  suspend fun updatePassword(password: PasswordEntity)

  @Delete
  suspend fun deletePassword(password: PasswordEntity)
}
