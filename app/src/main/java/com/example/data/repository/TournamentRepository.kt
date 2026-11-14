package com.example.data.repository

import com.example.data.local.CourtEntity
import com.example.data.local.FinancialSummary
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TeamEntity
import com.example.data.local.TeamStanding
import com.example.data.local.TournamentDao
import com.example.data.local.TransactionEntity
import com.example.domain.BwfRallyUpdateResult
import com.example.domain.BwfScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TournamentRepository(private val dao: TournamentDao) {

    val allPlayers: Flow<List<PlayerEntity>> = dao.getAllPlayers()
    val allTeams: Flow<List<TeamEntity>> = dao.getAllTeams()
    val allCourts: Flow<List<CourtEntity>> = dao.getAllCourts()
    val allMatches: Flow<List<MatchEntity>> = dao.getAllMatches()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    val financialSummary: Flow<FinancialSummary> = allTransactions.map { list ->
        var collected = 0.0
        var pending = 0.0
        var expenses = 0.0
        for (tx in list) {
            if (tx.type == "ENTRY_FEE") {
                collected += tx.amountPaid
                pending += (tx.amountExpected - tx.amountPaid).coerceAtLeast(0.0)
            } else if (tx.type == "EXPENSE") {
                expenses += tx.amountPaid
            }
        }
        FinancialSummary(
            totalCollected = collected,
            totalPendingDues = pending,
            totalExpenses = expenses
        )
    }

    suspend fun ensureSeedData() {
        if (dao.getPlayerCount() > 0) return

        val now = System.currentTimeMillis()
        val courts = listOf(
            CourtEntity(id = 1, courtName = "Court 1 • Arena Center", surfaceType = "Yonex Pro Mat", isAvailable = true),
            CourtEntity(id = 2, courtName = "Court 2 • North Wing", surfaceType = "BWF Synthetic Mat", isAvailable = true),
            CourtEntity(id = 3, courtName = "Court 3 • South Wing", surfaceType = "Hardwood Sprung", isAvailable = true)
        )
        dao.insertCourts(courts)

        val players = listOf(
            PlayerEntity(id = 1, name = "Aarav Sharma", phone = "+919876543210", skillTier = "Advanced", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", createdAt = now - 3600_000 * 10),
            PlayerEntity(id = 2, name = "Rohan Verma", phone = "+919876543211", skillTier = "Advanced", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", createdAt = now - 3600_000 * 9),
            PlayerEntity(id = 3, name = "Vikram Nair", phone = "+919876543212", skillTier = "Advanced", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 300.0, paymentStatus = "Partially Paid", paymentMode = "Cash", createdAt = now - 3600_000 * 8),
            PlayerEntity(id = 4, name = "Karan Malhotra", phone = "+919876543213", skillTier = "Intermediate", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI", createdAt = now - 3600_000 * 7),
            PlayerEntity(id = 5, name = "Siddharth Rao", phone = "+919876543214", skillTier = "Intermediate", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "Bank Transfer", createdAt = now - 3600_000 * 6),
            PlayerEntity(id = 6, name = "Devansh Patel", phone = "+919876543215", skillTier = "Intermediate", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", createdAt = now - 3600_000 * 5),
            PlayerEntity(id = 7, name = "Arjun Sen", phone = "+919876543216", skillTier = "Advanced", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", createdAt = now - 3600_000 * 4),
            PlayerEntity(id = 8, name = "Nikhil Joshi", phone = "+919876543217", skillTier = "Intermediate", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 200.0, paymentStatus = "Partially Paid", paymentMode = "Cash", createdAt = now - 3600_000 * 3),
            PlayerEntity(id = 9, name = "Pranav Kulkarni", phone = "+919876543218", skillTier = "Beginner", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI", createdAt = now - 3600_000 * 2),
            PlayerEntity(id = 10, name = "Harsh Vardhan", phone = "+919876543219", skillTier = "Beginner", category = "Men's Doubles", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "Cash", createdAt = now - 3600_000 * 1),
            PlayerEntity(id = 11, name = "Ananya Iyer", phone = "+919876543220", skillTier = "Advanced", category = "Mixed Doubles", registrationStatus = "Waitlisted", entryFeeTotal = 600.0, entryFeePaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI", createdAt = now - 1800_000),
            PlayerEntity(id = 12, name = "Meera Krishnan", phone = "+919876543221", skillTier = "Intermediate", category = "Women's Singles", registrationStatus = "Confirmed", entryFeeTotal = 500.0, entryFeePaid = 500.0, paymentStatus = "Paid", paymentMode = "UPI", createdAt = now - 900_000)
        )
        dao.insertPlayers(players)

        val teams = listOf(
            TeamEntity(id = 1, teamName = "Smash Titans", category = "Men's Doubles", player1Id = 1, player1Name = "Aarav Sharma", player2Id = 2, player2Name = "Rohan Verma", groupName = "Group A", registrationStatus = "Confirmed", seedRank = 1),
            TeamEntity(id = 2, teamName = "DropShot Kings", category = "Men's Doubles", player1Id = 3, player1Name = "Vikram Nair", player2Id = 4, player2Name = "Karan Malhotra", groupName = "Group A", registrationStatus = "Confirmed", seedRank = 2),
            TeamEntity(id = 3, teamName = "Net Ninjas", category = "Men's Doubles", player1Id = 5, player1Name = "Siddharth Rao", player2Id = 6, player2Name = "Devansh Patel", groupName = "Group A", registrationStatus = "Confirmed", seedRank = 3),
            TeamEntity(id = 4, teamName = "Feather Falcons", category = "Men's Doubles", player1Id = 7, player1Name = "Arjun Sen", player2Id = 8, player2Name = "Nikhil Joshi", groupName = "Group B", registrationStatus = "Confirmed", seedRank = 1),
            TeamEntity(id = 5, teamName = "Baseline Strikers", category = "Men's Doubles", player1Id = 9, player1Name = "Pranav Kulkarni", player2Id = 10, player2Name = "Harsh Vardhan", groupName = "Group B", registrationStatus = "Confirmed", seedRank = 2)
        )
        dao.insertTeams(teams)

        val matches = listOf(
            MatchEntity(
                id = 1,
                matchNumber = 1,
                stage = "Group Stage",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Group A",
                team1Id = 1,
                team1Name = "Smash Titans",
                team1Players = "Aarav S. & Rohan V.",
                team2Id = 2,
                team2Name = "DropShot Kings",
                team2Players = "Vikram N. & Karan M.",
                courtName = "Court 1 • Arena Center",
                scheduledTime = "09:00 AM",
                estimatedDurationMin = 35,
                status = "Completed",
                g1Team1 = 21,
                g1Team2 = 17,
                g2Team1 = 21,
                g2Team2 = 19,
                currentGame = 2,
                servingTeam = 1,
                intervalReachedG1 = true,
                intervalReachedG2 = true,
                winnerTeamId = 1
            ),
            MatchEntity(
                id = 2,
                matchNumber = 2,
                stage = "Group Stage",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Group B",
                team1Id = 4,
                team1Name = "Feather Falcons",
                team1Players = "Arjun S. & Nikhil J.",
                team2Id = 5,
                team2Name = "Baseline Strikers",
                team2Players = "Pranav K. & Harsh V.",
                courtName = "Court 2 • North Wing",
                scheduledTime = "09:40 AM",
                estimatedDurationMin = 35,
                status = "Completed",
                g1Team1 = 21,
                g1Team2 = 14,
                g2Team1 = 18,
                g2Team2 = 21,
                g3Team1 = 21,
                g3Team2 = 16,
                currentGame = 3,
                servingTeam = 1,
                intervalReachedG1 = true,
                intervalReachedG2 = true,
                intervalReachedG3 = true,
                winnerTeamId = 4
            ),
            MatchEntity(
                id = 3,
                matchNumber = 3,
                stage = "Group Stage",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Group A",
                team1Id = 2,
                team1Name = "DropShot Kings",
                team1Players = "Vikram N. & Karan M.",
                team2Id = 3,
                team2Name = "Net Ninjas",
                team2Players = "Siddharth R. & Devansh P.",
                courtName = "Court 1 • Arena Center",
                scheduledTime = "10:20 AM",
                estimatedDurationMin = 35,
                status = "Live",
                g1Team1 = 21,
                g1Team2 = 18,
                g2Team1 = 19,
                g2Team2 = 21,
                g3Team1 = 20,
                g3Team2 = 19,
                currentGame = 3,
                servingTeam = 1,
                intervalReachedG1 = true,
                intervalReachedG2 = true,
                intervalReachedG3 = true,
                rallyHistory = "3:1,3:2,3:1",
                winnerTeamId = null
            ),
            MatchEntity(
                id = 4,
                matchNumber = 4,
                stage = "Group Stage",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Group A",
                team1Id = 1,
                team1Name = "Smash Titans",
                team1Players = "Aarav S. & Rohan V.",
                team2Id = 3,
                team2Name = "Net Ninjas",
                team2Players = "Siddharth R. & Devansh P.",
                courtName = "Court 3 • South Wing",
                scheduledTime = "11:05 AM",
                estimatedDurationMin = 35,
                status = "Scheduled",
                currentGame = 1,
                servingTeam = 1
            ),
            MatchEntity(
                id = 5,
                matchNumber = 5,
                stage = "Semi-Final 1",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Knockout",
                team1Id = 1,
                team1Name = "Smash Titans",
                team1Players = "Aarav S. & Rohan V.",
                team2Id = 5,
                team2Name = "Baseline Strikers",
                team2Players = "Pranav K. & Harsh V.",
                courtName = "Court 1 • Arena Center",
                scheduledTime = "12:15 PM",
                estimatedDurationMin = 45,
                status = "Scheduled",
                nextMatchNumber = 7,
                nextMatchSlot = 1
            ),
            MatchEntity(
                id = 6,
                matchNumber = 6,
                stage = "Semi-Final 2",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Knockout",
                team1Id = 4,
                team1Name = "Feather Falcons",
                team1Players = "Arjun S. & Nikhil J.",
                team2Id = 2,
                team2Name = "DropShot Kings",
                team2Players = "Vikram N. & Karan M.",
                courtName = "Court 2 • North Wing",
                scheduledTime = "12:15 PM",
                estimatedDurationMin = 45,
                status = "Scheduled",
                nextMatchNumber = 7,
                nextMatchSlot = 2
            ),
            MatchEntity(
                id = 7,
                matchNumber = 7,
                stage = "Final",
                format = "Group + Knockout",
                category = "Men's Doubles",
                groupName = "Knockout",
                team1Id = 0,
                team1Name = "Winner SF 1",
                team1Players = "Awaiting Semi-Final 1",
                team2Id = 0,
                team2Name = "Winner SF 2",
                team2Players = "Awaiting Semi-Final 2",
                courtName = "Court 1 • Arena Center",
                scheduledTime = "02:00 PM",
                estimatedDurationMin = 50,
                status = "Scheduled"
            )
        )
        dao.insertMatches(matches)

        val transactions = listOf(
            TransactionEntity(id = 1, title = "Entry Fee • Aarav Sharma", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 1, playerPhone = "+919876543210", teamName = "Smash Titans", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "GPay #4910", timestamp = now - 3600_000 * 10),
            TransactionEntity(id = 2, title = "Entry Fee • Rohan Verma", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 2, playerPhone = "+919876543211", teamName = "Smash Titans", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "PhonePe #8123", timestamp = now - 3600_000 * 9),
            TransactionEntity(id = 3, title = "Entry Fee • Vikram Nair", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 3, playerPhone = "+919876543212", teamName = "DropShot Kings", amountExpected = 600.0, amountPaid = 300.0, paymentStatus = "Partially Paid", paymentMode = "Cash", notes = "Balance ₹300 due before Semi-Final", timestamp = now - 3600_000 * 8),
            TransactionEntity(id = 4, title = "Entry Fee • Karan Malhotra", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 4, playerPhone = "+919876543213", teamName = "DropShot Kings", amountExpected = 600.0, amountPaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI", notes = "Payment reminder pending", timestamp = now - 3600_000 * 7),
            TransactionEntity(id = 5, title = "Entry Fee • Siddharth Rao", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 5, playerPhone = "+919876543214", teamName = "Net Ninjas", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "Bank Transfer", notes = "IMPS settled", timestamp = now - 3600_000 * 6),
            TransactionEntity(id = 6, title = "Entry Fee • Devansh Patel", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 6, playerPhone = "+919876543215", teamName = "Net Ninjas", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "Paid on desk", timestamp = now - 3600_000 * 5),
            TransactionEntity(id = 7, title = "Entry Fee • Arjun Sen", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 7, playerPhone = "+919876543216", teamName = "Feather Falcons", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "Paid in full", timestamp = now - 3600_000 * 4),
            TransactionEntity(id = 8, title = "Entry Fee • Nikhil Joshi", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 8, playerPhone = "+919876543217", teamName = "Feather Falcons", amountExpected = 600.0, amountPaid = 200.0, paymentStatus = "Partially Paid", paymentMode = "Cash", notes = "Advance paid at desk", timestamp = now - 3600_000 * 3),
            TransactionEntity(id = 9, title = "Entry Fee • Pranav Kulkarni", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 9, playerPhone = "+919876543218", teamName = "Baseline Strikers", amountExpected = 600.0, amountPaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI", notes = "Promises UPI transfer today", timestamp = now - 3600_000 * 2),
            TransactionEntity(id = 10, title = "Entry Fee • Harsh Vardhan", type = "ENTRY_FEE", expenseCategory = "Entry Fee", playerId = 10, playerPhone = "+919876543219", teamName = "Baseline Strikers", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "Cash", notes = "Paid cash", timestamp = now - 3600_000 * 1),
            TransactionEntity(id = 11, title = "3 Indoor Synthetic Courts (6 Hrs)", type = "EXPENSE", expenseCategory = "Court Booking", amountExpected = 1800.0, amountPaid = 1800.0, paymentStatus = "Paid", paymentMode = "Bank Transfer", notes = "Arena booking receipt #CB-92", timestamp = now - 3600_000 * 12),
            TransactionEntity(id = 12, title = "Yonex Aerosensa-30 Feather Tubes (x3)", type = "EXPENSE", expenseCategory = "Shuttlecocks", amountExpected = 950.0, amountPaid = 950.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "36 tournament grade shuttles", timestamp = now - 3600_000 * 11),
            TransactionEntity(id = 13, title = "Winner & Runner-up Gold Trophies + Medals", type = "EXPENSE", expenseCategory = "Trophies", amountExpected = 600.0, amountPaid = 600.0, paymentStatus = "Paid", paymentMode = "Cash", notes = "Engraved championship cups", timestamp = now - 3600_000 * 8),
            TransactionEntity(id = 14, title = "Electrolyte Drinks & Energy Bananas", type = "EXPENSE", expenseCategory = "Refreshments", amountExpected = 350.0, amountPaid = 350.0, paymentStatus = "Paid", paymentMode = "UPI", notes = "Court-side hydration crate", timestamp = now - 3600_000 * 4)
        )
        dao.insertTransactions(transactions)
    }

    suspend fun registerPlayerWithFee(
        name: String,
        phone: String,
        skillTier: String,
        category: String,
        registrationStatus: String,
        entryFeeTotal: Double,
        entryFeePaid: Double,
        paymentMode: String
    ) {
        val payStatus = when {
            entryFeePaid >= entryFeeTotal -> "Paid"
            entryFeePaid > 0.0 -> "Partially Paid"
            else -> "Pending"
        }
        val playerId = dao.insertPlayer(
            PlayerEntity(
                name = name.trim(),
                phone = phone.trim(),
                skillTier = skillTier,
                category = category,
                registrationStatus = registrationStatus,
                entryFeeTotal = entryFeeTotal,
                entryFeePaid = entryFeePaid,
                paymentStatus = payStatus,
                paymentMode = paymentMode
            )
        )
        if (registrationStatus != "Withdrawn") {
            dao.insertTransaction(
                TransactionEntity(
                    title = "Entry Fee • ${name.trim()}",
                    type = "ENTRY_FEE",
                    expenseCategory = "Entry Fee",
                    playerId = playerId,
                    playerPhone = phone.trim(),
                    teamName = category,
                    amountExpected = entryFeeTotal,
                    amountPaid = entryFeePaid,
                    paymentStatus = payStatus,
                    paymentMode = paymentMode,
                    notes = "$skillTier • $category"
                )
            )
        }
    }

    suspend fun updatePlayerStatus(player: PlayerEntity, newStatus: String) {
        dao.updatePlayer(player.copy(registrationStatus = newStatus))
    }

    suspend fun updatePlayerDetails(
        player: PlayerEntity,
        name: String,
        phone: String,
        skillTier: String,
        category: String,
        registrationStatus: String,
        entryFeeTotal: Double,
        entryFeePaid: Double,
        paymentMode: String
    ) {
        val payStatus = when {
            entryFeePaid >= entryFeeTotal -> "Paid"
            entryFeePaid > 0.0 -> "Partially Paid"
            else -> "Pending"
        }
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val updatedPlayer = player.copy(
            name = cleanName,
            phone = cleanPhone,
            skillTier = skillTier,
            category = category,
            registrationStatus = registrationStatus,
            entryFeeTotal = entryFeeTotal,
            entryFeePaid = entryFeePaid,
            paymentStatus = payStatus,
            paymentMode = paymentMode
        )
        dao.updatePlayer(updatedPlayer)

        // Update linked entry fee transaction if one exists
        val txList = dao.getAllTransactionsSnapshot()
        val linkedTx = txList.find { it.playerId == player.id && it.type == "ENTRY_FEE" }
        if (linkedTx != null) {
            dao.updateTransaction(
                linkedTx.copy(
                    title = "Entry Fee • $cleanName",
                    playerPhone = cleanPhone,
                    teamName = category,
                    amountExpected = entryFeeTotal,
                    amountPaid = entryFeePaid,
                    paymentStatus = payStatus,
                    paymentMode = paymentMode,
                    notes = "$skillTier • $category",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        dao.deletePlayer(player)
        val txList = dao.getAllTransactionsSnapshot()
        txList.filter { it.playerId == player.id }.forEach { tx ->
            dao.deleteTransaction(tx)
        }
    }

    suspend fun addSimplePlayer(name: String, perPlayerShare: Double) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        dao.insertPlayer(
            PlayerEntity(
                name = cleanName,
                phone = "",
                skillTier = "Member",
                category = "Monthly Court",
                registrationStatus = "Confirmed",
                entryFeeTotal = perPlayerShare,
                entryFeePaid = 0.0,
                paymentStatus = "Unpaid",
                paymentMode = "UPI"
            )
        )
    }

    suspend fun togglePlayerPaidStatus(player: PlayerEntity, perPlayerShare: Double) {
        // Cycles: Paid -> Unpaid -> Inactive -> Paid
        val nextStatus = when (player.paymentStatus) {
            "Paid" -> "Unpaid"
            "Unpaid" -> "Inactive"
            "Inactive" -> "Paid"
            else -> "Paid"
        }
        val isPaid = nextStatus == "Paid"
        val updated = player.copy(
            entryFeeTotal = perPlayerShare,
            entryFeePaid = if (isPaid) perPlayerShare else 0.0,
            paymentStatus = nextStatus,
            registrationStatus = if (nextStatus == "Inactive") "Waitlisted" else "Confirmed"
        )
        dao.updatePlayer(updated)
    }

    suspend fun setPlayerStatus(player: PlayerEntity, status: String, perPlayerShare: Double) {
        val cleanStatus = when (status) {
            "Paid" -> "Paid"
            "Inactive" -> "Inactive"
            else -> "Unpaid"
        }
        val isPaid = cleanStatus == "Paid"
        val updated = player.copy(
            entryFeeTotal = perPlayerShare,
            entryFeePaid = if (isPaid) perPlayerShare else 0.0,
            paymentStatus = cleanStatus,
            registrationStatus = if (cleanStatus == "Inactive") "Waitlisted" else "Confirmed"
        )
        dao.updatePlayer(updated)
    }

    suspend fun renamePlayer(player: PlayerEntity, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        dao.updatePlayer(player.copy(name = cleanName))
    }

    suspend fun setAllPlayersPaidStatus(paid: Boolean, perPlayerShare: Double) {
        val all = dao.getAllPlayersSnapshot()
        for (p in all) {
            // Keep inactive players as Inactive, only update non-inactive or all if resetting
            val targetStatus = if (paid) "Paid" else "Unpaid"
            dao.updatePlayer(
                p.copy(
                    entryFeeTotal = perPlayerShare,
                    entryFeePaid = if (paid) perPlayerShare else 0.0,
                    paymentStatus = targetStatus
                )
            )
        }
    }

    suspend fun createTeam(
        customTeamName: String,
        category: String,
        player1: PlayerEntity,
        player2: PlayerEntity?,
        groupName: String,
        registrationStatus: String
    ) {
        val generatedName = if (customTeamName.isNotBlank()) {
            customTeamName.trim()
        } else if (player2 != null) {
            val p1First = player1.name.substringBefore(" ")
            val p2First = player2.name.substringBefore(" ")
            "$p1First / $p2First Duo"
        } else {
            player1.name
        }

        dao.insertTeam(
            TeamEntity(
                teamName = generatedName,
                category = category,
                player1Id = player1.id,
                player1Name = player1.name,
                player2Id = player2?.id,
                player2Name = player2?.name,
                groupName = groupName,
                registrationStatus = registrationStatus
            )
        )
    }

    suspend fun updateTeamDetails(
        team: TeamEntity,
        customTeamName: String,
        category: String,
        player1: PlayerEntity,
        player2: PlayerEntity?,
        groupName: String,
        registrationStatus: String
    ) {
        val generatedName = if (customTeamName.isNotBlank()) {
            customTeamName.trim()
        } else if (player2 != null) {
            val p1First = player1.name.substringBefore(" ")
            val p2First = player2.name.substringBefore(" ")
            "$p1First / $p2First Duo"
        } else {
            player1.name
        }

        val updatedTeam = team.copy(
            teamName = generatedName,
            category = category,
            player1Id = player1.id,
            player1Name = player1.name,
            player2Id = player2?.id,
            player2Name = player2?.name,
            groupName = groupName,
            registrationStatus = registrationStatus
        )
        dao.updateTeam(updatedTeam)

        // Sync team name and players in existing matches
        val matches = dao.getAllMatchesSnapshot()
        val playersLabel = formatTeamPlayers(updatedTeam)
        for (m in matches) {
            if (m.team1Id == team.id) {
                dao.updateMatch(m.copy(team1Name = updatedTeam.teamName, team1Players = playersLabel))
            } else if (m.team2Id == team.id) {
                dao.updateMatch(m.copy(team2Name = updatedTeam.teamName, team2Players = playersLabel))
            }
        }
    }

    suspend fun deleteTeam(team: TeamEntity) {
        dao.deleteTeam(team)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun updateTeamRegistrationStatus(team: TeamEntity, status: String) {
        dao.updateTeam(team.copy(registrationStatus = status))
    }

    suspend fun recordPaymentUpdate(
        transaction: TransactionEntity,
        newAmountPaid: Double,
        paymentMode: String
    ) {
        val cappedPaid = newAmountPaid.coerceIn(0.0, transaction.amountExpected)
        val newStatus = when {
            cappedPaid >= transaction.amountExpected -> "Paid"
            cappedPaid > 0.0 -> "Partially Paid"
            else -> "Pending"
        }
        dao.updateTransaction(
            transaction.copy(
                amountPaid = cappedPaid,
                paymentStatus = newStatus,
                paymentMode = paymentMode,
                timestamp = System.currentTimeMillis()
            )
        )
        // Also update linked player if present
        if (transaction.playerId != null) {
            val players = dao.getAllPlayersSnapshot()
            val player = players.find { it.id == transaction.playerId }
            if (player != null) {
                dao.updatePlayer(
                    player.copy(
                        entryFeePaid = cappedPaid,
                        paymentStatus = newStatus,
                        paymentMode = paymentMode
                    )
                )
            }
        }
    }

    suspend fun addExpenseOrEntryTransaction(
        title: String,
        type: String,
        expenseCategory: String,
        playerPhone: String,
        teamName: String,
        amountExpected: Double,
        amountPaid: Double,
        paymentMode: String,
        notes: String
    ) {
        val status = when {
            amountPaid >= amountExpected -> "Paid"
            amountPaid > 0.0 -> "Partially Paid"
            else -> "Pending"
        }
        dao.insertTransaction(
            TransactionEntity(
                title = title.trim(),
                type = type,
                expenseCategory = expenseCategory,
                playerPhone = playerPhone.trim(),
                teamName = teamName.trim(),
                amountExpected = amountExpected,
                amountPaid = amountPaid,
                paymentStatus = status,
                paymentMode = paymentMode,
                notes = notes.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Scores a point on a match using BwfScoringEngine and automatically advances
     * knockout winners (or top group stage winners into knockout slots) upon completion.
     */
    suspend fun scoreRallyPoint(match: MatchEntity, scoringTeam: Int): BwfRallyUpdateResult {
        val result = BwfScoringEngine.scorePoint(match, scoringTeam)
        dao.updateMatch(result.updatedMatch)

        if (result.matchJustWonBy != null) {
            advanceWinnerIfNeeded(result.updatedMatch)
        }
        return result
    }

    suspend fun undoLastRallyPoint(match: MatchEntity): MatchEntity {
        val updated = BwfScoringEngine.undoLastPoint(match)
        dao.updateMatch(updated)
        return updated
    }

    suspend fun switchServerSideManually(match: MatchEntity) {
        val nextServer = if (match.servingTeam == 1) 2 else 1
        dao.updateMatch(match.copy(servingTeam = nextServer, updatedAt = System.currentTimeMillis()))
    }

    suspend fun startMatchLive(match: MatchEntity) {
        dao.updateMatch(match.copy(status = "Live", updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateMatchCourtAndTime(
        match: MatchEntity,
        courtName: String,
        scheduledTime: String,
        durationMin: Int
    ) {
        dao.updateMatch(
            match.copy(
                courtName = courtName,
                scheduledTime = scheduledTime,
                estimatedDurationMin = durationMin,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun advanceWinnerIfNeeded(completedMatch: MatchEntity) {
        val winnerId = completedMatch.winnerTeamId ?: return
        val winnerName = if (winnerId == completedMatch.team1Id) completedMatch.team1Name else completedMatch.team2Name
        val winnerPlayers = if (winnerId == completedMatch.team1Id) completedMatch.team1Players else completedMatch.team2Players

        val nextNum = completedMatch.nextMatchNumber
        val nextSlot = completedMatch.nextMatchSlot
        if (nextNum != null && nextSlot != null) {
            val targetMatch = dao.getMatchByNumber(nextNum)
            if (targetMatch != null) {
                val updatedTarget = if (nextSlot == 1) {
                    targetMatch.copy(
                        team1Id = winnerId,
                        team1Name = winnerName,
                        team1Players = winnerPlayers
                    )
                } else {
                    targetMatch.copy(
                        team2Id = winnerId,
                        team2Name = winnerName,
                        team2Players = winnerPlayers
                    )
                }
                dao.updateMatch(updatedTarget)
            }
        }
    }

    suspend fun getConfirmedTeamsSnapshot(): List<TeamEntity> {
        return dao.getConfirmedTeamsSnapshot()
    }

    suspend fun getAvailableCourtsSnapshot(): List<CourtEntity> {
        return dao.getAllCourtsSnapshot().filter { it.isAvailable }.ifEmpty {
            listOf(CourtEntity(id = 1, courtName = "Court 1 • Arena Center"))
        }
    }

    suspend fun replaceAllMatches(matches: List<MatchEntity>) {
        dao.clearAllMatches()
        if (matches.isNotEmpty()) {
            dao.insertMatches(matches)
        }
    }

    /**
     * Auto-generates tournament fixtures based on registered Confirmed teams.
     * Supports:
     * - "Round Robin" (Group Stage all-play-all)
     * - "Knockout" (Single Elimination Semi-Finals + Final)
     * - "Group + Knockout" (Group Stage followed by Semi-Finals + Championship Final)
     * Implements back-to-back team conflict avoidance and automatic Court & Time Slot allocation.
     */
    suspend fun generateTournamentFixtures(
        format: String,
        startHour: Int = 9,
        startMinute: Int = 0,
        slotDurationMin: Int = 35
    ): Int {
        val confirmedTeams = getConfirmedTeamsSnapshot()
        if (confirmedTeams.size < 2) return 0

        val courts = getAvailableCourtsSnapshot()

        val rawPairings = mutableListOf<Triple<String, TeamEntity, TeamEntity>>()

        when (format) {
            "Round Robin" -> {
                for (i in confirmedTeams.indices) {
                    for (j in i + 1 until confirmedTeams.size) {
                        val t1 = confirmedTeams[i]
                        val t2 = confirmedTeams[j]
                        val grp = if (t1.groupName == t2.groupName) t1.groupName else "Open League"
                        rawPairings.add(Triple(grp, t1, t2))
                    }
                }
            }
            "Knockout" -> {
                val pool = confirmedTeams.take(4)
                if (pool.size >= 4) {
                    rawPairings.add(Triple("Semi-Final 1", pool[0], pool[3]))
                    rawPairings.add(Triple("Semi-Final 2", pool[1], pool[2]))
                } else {
                    rawPairings.add(Triple("Final", pool[0], pool[1]))
                }
            }
            else -> { // "Group + Knockout"
                val byGroup = confirmedTeams.groupBy { it.groupName }
                for ((grp, grpTeams) in byGroup) {
                    for (i in grpTeams.indices) {
                        for (j in i + 1 until grpTeams.size) {
                            rawPairings.add(Triple(grp, grpTeams[i], grpTeams[j]))
                        }
                    }
                }
                if (rawPairings.isEmpty()) {
                    for (i in confirmedTeams.indices) {
                        for (j in i + 1 until confirmedTeams.size) {
                            rawPairings.add(Triple("Group A", confirmedTeams[i], confirmedTeams[j]))
                        }
                    }
                }
            }
        }

        // Reorder pairings greedy-style to avoid back-to-back match conflicts for the same team
        val orderedPairings = avoidBackToBackConflicts(rawPairings)

        dao.clearAllMatches()

        val generatedMatches = mutableListOf<MatchEntity>()
        var matchNumber = 1
        var elapsedMinutes = 0

        for ((index, pairing) in orderedPairings.withIndex()) {
            val (stageOrGroup, t1, t2) = pairing
            val court = courts[index % courts.size]
            if (index > 0 && index % courts.size == 0) {
                elapsedMinutes += slotDurationMin + 5 // 5 min court changeover buffer
            }
            val timeStr = formatClockTime(startHour, startMinute + elapsedMinutes)
            val isKnockoutStage = stageOrGroup.startsWith("Semi-Final") || stageOrGroup == "Final"

            generatedMatches.add(
                MatchEntity(
                    matchNumber = matchNumber,
                    stage = if (isKnockoutStage) stageOrGroup else "Group Stage",
                    format = format,
                    category = t1.category,
                    groupName = if (isKnockoutStage) "Knockout" else stageOrGroup,
                    team1Id = t1.id,
                    team1Name = t1.teamName,
                    team1Players = formatTeamPlayers(t1),
                    team2Id = t2.id,
                    team2Name = t2.teamName,
                    team2Players = formatTeamPlayers(t2),
                    courtName = court.courtName,
                    scheduledTime = timeStr,
                    estimatedDurationMin = slotDurationMin,
                    status = if (matchNumber == 1) "Live" else "Scheduled",
                    nextMatchNumber = if (format == "Knockout" && stageOrGroup.startsWith("Semi-Final")) 3 else null,
                    nextMatchSlot = when (stageOrGroup) {
                        "Semi-Final 1" -> 1
                        "Semi-Final 2" -> 2
                        else -> null
                    }
                )
            )
            matchNumber++
        }

        // Append Knockout Bracket matches for "Knockout" or "Group + Knockout"
        if (format == "Knockout" && orderedPairings.size == 2) {
            elapsedMinutes += slotDurationMin + 15
            generatedMatches.add(
                MatchEntity(
                    matchNumber = matchNumber,
                    stage = "Final",
                    format = format,
                    category = confirmedTeams.first().category,
                    groupName = "Knockout",
                    team1Id = 0,
                    team1Name = "Winner SF 1",
                    team1Players = "Semi-Final 1 Winner",
                    team2Id = 0,
                    team2Name = "Winner SF 2",
                    team2Players = "Semi-Final 2 Winner",
                    courtName = courts.first().courtName,
                    scheduledTime = formatClockTime(startHour, startMinute + elapsedMinutes),
                    estimatedDurationMin = 45,
                    status = "Scheduled"
                )
            )
        } else if (format == "Group + Knockout" && confirmedTeams.size >= 4) {
            elapsedMinutes += slotDurationMin + 15
            val sf1Num = matchNumber++
            val sf2Num = matchNumber++
            val finalNum = matchNumber

            val sfTime = formatClockTime(startHour, startMinute + elapsedMinutes)
            val tA1 = confirmedTeams.getOrNull(0)
            val tB2 = confirmedTeams.getOrNull(3) ?: confirmedTeams.getOrNull(1)
            val tB1 = confirmedTeams.getOrNull(1)
            val tA2 = confirmedTeams.getOrNull(2)

            generatedMatches.add(
                MatchEntity(
                    matchNumber = sf1Num,
                    stage = "Semi-Final 1",
                    format = format,
                    category = confirmedTeams.first().category,
                    groupName = "Knockout",
                    team1Id = tA1?.id ?: 0,
                    team1Name = tA1?.teamName ?: "#1 Group A",
                    team1Players = tA1?.let { formatTeamPlayers(it) } ?: "Top Seed Group A",
                    team2Id = tB2?.id ?: 0,
                    team2Name = tB2?.teamName ?: "#2 Group B",
                    team2Players = tB2?.let { formatTeamPlayers(it) } ?: "Runner-up Group B",
                    courtName = courts.first().courtName,
                    scheduledTime = sfTime,
                    estimatedDurationMin = 45,
                    status = "Scheduled",
                    nextMatchNumber = finalNum,
                    nextMatchSlot = 1
                )
            )
            generatedMatches.add(
                MatchEntity(
                    matchNumber = sf2Num,
                    stage = "Semi-Final 2",
                    format = format,
                    category = confirmedTeams.first().category,
                    groupName = "Knockout",
                    team1Id = tB1?.id ?: 0,
                    team1Name = tB1?.teamName ?: "#1 Group B",
                    team1Players = tB1?.let { formatTeamPlayers(it) } ?: "Top Seed Group B",
                    team2Id = tA2?.id ?: 0,
                    team2Name = tA2?.teamName ?: "#2 Group A",
                    team2Players = tA2?.let { formatTeamPlayers(it) } ?: "Runner-up Group A",
                    courtName = courts.getOrElse(1) { courts.first() }.courtName,
                    scheduledTime = sfTime,
                    estimatedDurationMin = 45,
                    status = "Scheduled",
                    nextMatchNumber = finalNum,
                    nextMatchSlot = 2
                )
            )

            elapsedMinutes += 50
            generatedMatches.add(
                MatchEntity(
                    matchNumber = finalNum,
                    stage = "Final",
                    format = format,
                    category = confirmedTeams.first().category,
                    groupName = "Knockout",
                    team1Id = 0,
                    team1Name = "Winner SF 1",
                    team1Players = "Advances from Semi-Final 1",
                    team2Id = 0,
                    team2Name = "Winner SF 2",
                    team2Players = "Advances from Semi-Final 2",
                    courtName = courts.first().courtName,
                    scheduledTime = formatClockTime(startHour, startMinute + elapsedMinutes),
                    estimatedDurationMin = 50,
                    status = "Scheduled"
                )
            )
        }

        dao.insertMatches(generatedMatches)
        return generatedMatches.size
    }

    private fun avoidBackToBackConflicts(
        pairings: List<Triple<String, TeamEntity, TeamEntity>>
    ): List<Triple<String, TeamEntity, TeamEntity>> {
        val remaining = pairings.toMutableList()
        val result = mutableListOf<Triple<String, TeamEntity, TeamEntity>>()
        var lastTeams = emptySet<Long>()

        while (remaining.isNotEmpty()) {
            val nonConflictingIndex = remaining.indexOfFirst { (_, t1, t2) ->
                t1.id !in lastTeams && t2.id !in lastTeams
            }
            val pick = if (nonConflictingIndex >= 0) {
                remaining.removeAt(nonConflictingIndex)
            } else {
                remaining.removeAt(0)
            }
            result.add(pick)
            lastTeams = setOf(pick.second.id, pick.third.id)
        }
        return result
    }

    private fun formatTeamPlayers(team: TeamEntity): String {
        return if (!team.player2Name.isNullOrBlank()) {
            "${team.player1Name} & ${team.player2Name}"
        } else {
            team.player1Name
        }
    }

    private fun formatClockTime(baseHour: Int, totalMinutesOffset: Int): String {
        val totalMins = baseHour * 60 + totalMinutesOffset
        val hour24 = (totalMins / 60) % 24
        val min = totalMins % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format("%02d:%02d %s", hour12, min, amPm)
    }

    fun calculateStandings(
        teams: List<TeamEntity>,
        matches: List<MatchEntity>
    ): List<TeamStanding> {
        val confirmedTeams = teams.filter { it.registrationStatus == "Confirmed" }
        val groupMatches = matches.filter { it.stage == "Group Stage" && it.status == "Completed" }

        return confirmedTeams.map { team ->
            var played = 0
            var won = 0
            var lost = 0
            var gamesWon = 0
            var gamesLost = 0
            var ptsScored = 0
            var ptsConceded = 0

            for (m in groupMatches) {
                if (m.team1Id == team.id || m.team2Id == team.id) {
                    played++
                    val isTeam1 = m.team1Id == team.id
                    val (g1, g2) = BwfScoringEngine.getGamesWonCount(m)
                    if (isTeam1) {
                        gamesWon += g1
                        gamesLost += g2
                        ptsScored += m.g1Team1 + m.g2Team1 + m.g3Team1
                        ptsConceded += m.g1Team2 + m.g2Team2 + m.g3Team2
                    } else {
                        gamesWon += g2
                        gamesLost += g1
                        ptsScored += m.g1Team2 + m.g2Team2 + m.g3Team2
                        ptsConceded += m.g1Team1 + m.g2Team1 + m.g3Team1
                    }
                    if (m.winnerTeamId == team.id) won++ else lost++
                }
            }

            TeamStanding(
                teamId = team.id,
                teamName = team.teamName,
                playersLabel = formatTeamPlayers(team),
                groupName = team.groupName,
                played = played,
                won = won,
                lost = lost,
                gamesWon = gamesWon,
                gamesLost = gamesLost,
                pointsScored = ptsScored,
                pointsConceded = ptsConceded
            )
        }.sortedWith(
            compareBy<TeamStanding> { it.groupName }
                .thenByDescending { it.tablePoints }
                .thenByDescending { it.netGameDiff }
                .thenByDescending { it.netPointDiff }
        )
    }
}
