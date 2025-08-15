package com.example.gymmanagement.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: Member)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(members: List<Member>)

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberById(id: Int): Flow<Member?>

    @Query("SELECT * FROM members WHERE expiryDate >= :today AND expiryDate < :sevenDaysLater ORDER BY expiryDate ASC")
    fun getMembersExpiringSoon(today: Long, sevenDaysLater: Long): Flow<List<Member>>

    @Delete
    suspend fun deleteMember(member: Member)
}