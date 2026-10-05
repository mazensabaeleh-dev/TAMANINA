package com.example.data.db

import androidx.room.*
import com.example.data.model.CheckInRecord
import com.example.data.model.SchoolCycle
import com.example.data.model.SupportTicket
import com.example.data.model.TeamMember
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getCheckInsPaged(limit: Int, offset: Int): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_ins WHERE cycleId = :cycleId ORDER BY timestamp DESC")
    fun getCheckInsByCycle(cycleId: Long): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_ins WHERE cycleId = :cycleId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getCheckInsByCyclePaged(cycleId: Long, limit: Int, offset: Int): Flow<List<CheckInRecord>>

    @Query("SELECT COUNT(*) FROM check_ins")
    fun getTotalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckInRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkIns: List<CheckInRecord>)

    @Query("DELETE FROM check_ins WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM check_ins")
    suspend fun clearAll()
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY CASE urgency WHEN 'CRITICAL' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, timestamp DESC")
    fun getAllTickets(): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets ORDER BY CASE urgency WHEN 'CRITICAL' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, timestamp DESC LIMIT :limit OFFSET :offset")
    fun getTicketsPaged(limit: Int, offset: Int): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE status = :status ORDER BY timestamp DESC")
    fun getTicketsByStatus(status: String): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<SupportTicket>)

    @Update
    suspend fun updateTicket(ticket: SupportTicket)

    @Query("UPDATE support_tickets SET status = :status, counselorNotes = :counselorNotes, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateStatusAndNotes(id: Long, status: String, counselorNotes: String?, resolvedAt: Long?)

    @Query("DELETE FROM support_tickets WHERE id = :id")
    suspend fun deleteTicket(id: Long)

    @Query("DELETE FROM support_tickets WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}

@Dao
interface SchoolCycleDao {
    @Query("SELECT * FROM school_cycles ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<SchoolCycle>>

    @Query("SELECT * FROM school_cycles WHERE isActive = 1 LIMIT 1")
    fun getActiveCycle(): Flow<SchoolCycle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: SchoolCycle): Long

    @Query("UPDATE school_cycles SET isActive = 0")
    suspend fun deactivateAllCycles()

    @Update
    suspend fun updateCycle(cycle: SchoolCycle)
}

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members ORDER BY isOwner DESC, addedAt ASC")
    fun getAllTeamMembers(): Flow<List<TeamMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<TeamMember>)

    @Update
    suspend fun updateMember(member: TeamMember)

    @Query("DELETE FROM team_members WHERE id = :id AND isOwner = 0")
    suspend fun deleteMember(id: Long)
}
