package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [
        Index(value = ["skillTier"]),
        Index(value = ["category"]),
        Index(value = ["registrationStatus"]),
        Index(value = ["paymentStatus"])
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val skillTier: String, // "Beginner", "Intermediate", "Advanced"
    val category: String, // "Men's Singles", "Women's Singles", "Men's Doubles", "Women's Doubles", "Mixed Doubles"
    val registrationStatus: String, // "Confirmed", "Waitlisted", "Withdrawn"
    val entryFeeTotal: Double = 600.0,
    val entryFeePaid: Double = 600.0,
    val paymentStatus: String = "Paid", // "Paid", "Partially Paid", "Pending"
    val paymentMode: String = "UPI", // "UPI", "Cash", "Bank Transfer"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "teams",
    indices = [
        Index(value = ["category"]),
        Index(value = ["groupName"]),
        Index(value = ["registrationStatus"]),
        Index(value = ["player1Id", "player2Id"])
    ]
)
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamName: String,
    val category: String,
    val player1Id: Long,
    val player1Name: String,
    val player2Id: Long?,
    val player2Name: String?,
    val groupName: String = "Group A", // "Group A", "Group B"
    val registrationStatus: String = "Confirmed", // "Confirmed", "Waitlisted", "Withdrawn"
    val seedRank: Int = 1
)

@Entity(
    tableName = "courts",
    indices = [Index(value = ["courtName"], unique = true)]
)
data class CourtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courtName: String, // "Court 1", "Court 2", "Court 3"
    val surfaceType: String = "BWF Synthetic Mat",
    val isAvailable: Boolean = true
)

@Entity(
    tableName = "matches",
    indices = [
        Index(value = ["stage", "groupName"]),
        Index(value = ["status"]),
        Index(value = ["courtName", "scheduledTime"]),
        Index(value = ["team1Id", "team2Id"])
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchNumber: Int,
    val stage: String, // "Group Stage", "Semi-Final 1", "Semi-Final 2", "Final"
    val format: String = "Group + Knockout", // "Round Robin", "Knockout", "Group + Knockout"
    val category: String = "Men's Doubles",
    val groupName: String = "Group A", // "Group A", "Group B", "Knockout"
    val team1Id: Long,
    val team1Name: String,
    val team1Players: String,
    val team2Id: Long,
    val team2Name: String,
    val team2Players: String,
    val courtName: String,
    val scheduledTime: String, // e.g., "09:30 AM"
    val estimatedDurationMin: Int = 35,
    val status: String = "Scheduled", // "Scheduled", "Live", "Completed"
    val g1Team1: Int = 0,
    val g1Team2: Int = 0,
    val g2Team1: Int = 0,
    val g2Team2: Int = 0,
    val g3Team1: Int = 0,
    val g3Team2: Int = 0,
    val currentGame: Int = 1, // 1, 2, or 3
    val servingTeam: Int = 1, // 1 or 2
    val intervalReachedG1: Boolean = false,
    val intervalReachedG2: Boolean = false,
    val intervalReachedG3: Boolean = false,
    val winnerTeamId: Long? = null,
    val nextMatchNumber: Int? = null,
    val nextMatchSlot: Int? = null, // 1 for Team 1 slot, 2 for Team 2 slot
    val rallyHistory: String = "", // Comma-separated "1" or "2" for undo functionality
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["type"]),
        Index(value = ["paymentStatus"]),
        Index(value = ["paymentMode"]),
        Index(value = ["playerId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "ENTRY_FEE" or "EXPENSE"
    val expenseCategory: String, // "Entry Fee", "Court Booking", "Shuttlecocks", "Trophies", "Refreshments"
    val playerId: Long? = null,
    val playerPhone: String = "",
    val teamName: String = "",
    val amountExpected: Double,
    val amountPaid: Double,
    val paymentStatus: String, // "Paid", "Partially Paid", "Pending"
    val paymentMode: String, // "UPI", "Cash", "Bank Transfer"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TeamStanding(
    val teamId: Long,
    val teamName: String,
    val playersLabel: String,
    val groupName: String,
    val played: Int,
    val won: Int,
    val lost: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val pointsScored: Int,
    val pointsConceded: Int
) {
    val netGameDiff: Int get() = gamesWon - gamesLost
    val netPointDiff: Int get() = pointsScored - pointsConceded
    val tablePoints: Int get() = won * 2
}

data class FinancialSummary(
    val totalCollected: Double = 0.0,
    val totalPendingDues: Double = 0.0,
    val totalExpenses: Double = 0.0
) {
    val netBalance: Double get() = totalCollected - totalExpenses
}
