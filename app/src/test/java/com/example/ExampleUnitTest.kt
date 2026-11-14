package com.example

import com.example.data.local.CourtEntity
import com.example.data.local.FinancialSummary
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TeamEntity
import com.example.domain.BwfScoringEngine
import com.example.ui.viewmodel.TournamentViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun createSampleMatch(): MatchEntity {
        return MatchEntity(
            id = 1,
            matchNumber = 1,
            stage = "Group Stage",
            team1Id = 1,
            team1Name = "Smash Titans",
            team1Players = "Aarav & Rohan",
            team2Id = 2,
            team2Name = "DropShot Kings",
            team2Players = "Vikram & Karan",
            courtName = "Court 1",
            scheduledTime = "09:00 AM",
            status = "Live"
        )
    }

    @Test
    fun bwfStandard21PointAndIntervalRule() {
        var match = createSampleMatch()
        var intervalTriggeredCount = 0

        for (i in 1..11) {
            val res = BwfScoringEngine.scorePoint(match, 1)
            match = res.updatedMatch
            if (res.triggeredIntervalAlert) intervalTriggeredCount++
        }
        assertEquals(1, intervalTriggeredCount)
        assertEquals(11, match.g1Team1)

        // Score to 21-0 to win Game 1
        for (i in 12..21) {
            val res = BwfScoringEngine.scorePoint(match, 1)
            match = res.updatedMatch
        }
        assertEquals(2, match.currentGame)
        assertEquals(1 to 0, BwfScoringEngine.getGamesWonCount(match))
    }

    @Test
    fun bwfDeuceAndGoldenPointAt30Rule() {
        assertFalse(BwfScoringEngine.isGameWon(21, 20))
        assertTrue(BwfScoringEngine.isGameWon(22, 20))
        assertFalse(BwfScoringEngine.isGameWon(29, 28))
        assertTrue(BwfScoringEngine.isGameWon(30, 29))

        var match = createSampleMatch().copy(g1Team1 = 20, g1Team2 = 20)
        assertNotNull(BwfScoringEngine.getPointSituationLabel(match))
        match = BwfScoringEngine.scorePoint(match, 1).updatedMatch
        assertEquals(1, match.currentGame) // 21-20 does not win Game 1
        match = BwfScoringEngine.scorePoint(match, 1).updatedMatch
        assertEquals(2, match.currentGame) // 22-20 wins Game 1
    }

    @Test
    fun roundRobinFixtureGeneratorAvoidsBackToBackMatches() {
        val teams = (1L..6L).map { id ->
            TeamEntity(
                id = id,
                teamName = "Team $id",
                category = "Men's Doubles",
                player1Id = id * 2 - 1,
                player1Name = "Player A$id",
                player2Id = id * 2,
                player2Name = "Player B$id",
                groupName = "Group A",
                registrationStatus = "Confirmed"
            )
        }
        val courts = listOf(
            CourtEntity(id = 1, courtName = "Court 1"),
            CourtEntity(id = 2, courtName = "Court 2")
        )

        // Test 5 teams (10 matches) and 6 teams (15 matches)
        for (teamCount in listOf(5, 6)) {
            val subset = teams.take(teamCount)
            val fixtures = TournamentViewModel.buildTournamentFixtures(
                confirmedTeams = subset,
                availableCourts = courts,
                format = "Round Robin"
            )

            val expectedMatches = teamCount * (teamCount - 1) / 2
            assertEquals(expectedMatches, fixtures.size)

            // Verify no team plays back-to-back consecutive matches
            for (i in 1 until fixtures.size) {
                val prev = fixtures[i - 1]
                val curr = fixtures[i]
                val prevTeams = setOf(prev.team1Id, prev.team2Id)
                val currTeams = setOf(curr.team1Id, curr.team2Id)
                assertTrue(
                    "Teams overlapped between match #${prev.matchNumber} and #${curr.matchNumber}",
                    prevTeams.intersect(currTeams).isEmpty()
                )
            }
        }
    }

    @Test
    fun dashboardStatsStateComputesPlayedPendingAndActivePlayers() {
        val players = listOf(
            PlayerEntity(id = 1, name = "P1", phone = "1", skillTier = "Advanced", category = "MD", registrationStatus = "Confirmed", entryFeeTotal = 600.0, entryFeePaid = 600.0, paymentStatus = "Paid", paymentMode = "UPI"),
            PlayerEntity(id = 2, name = "P2", phone = "2", skillTier = "Intermediate", category = "MD", registrationStatus = "Waitlisted", entryFeeTotal = 600.0, entryFeePaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI"),
            PlayerEntity(id = 3, name = "P3", phone = "3", skillTier = "Beginner", category = "MD", registrationStatus = "Withdrawn", entryFeeTotal = 600.0, entryFeePaid = 0.0, paymentStatus = "Pending", paymentMode = "UPI")
        )
        val matches = listOf(
            createSampleMatch().copy(id = 1, matchNumber = 1, status = "Completed"),
            createSampleMatch().copy(id = 2, matchNumber = 2, status = "Live"),
            createSampleMatch().copy(id = 3, matchNumber = 3, status = "Scheduled")
        )
        val stats = TournamentViewModel.computeDashboardStats(
            players = players,
            teams = emptyList(),
            courts = emptyList(),
            matches = matches,
            financialSummary = FinancialSummary(totalCollected = 600.0, totalPendingDues = 600.0, totalExpenses = 200.0)
        )
        assertEquals(1, stats.totalMatchesPlayed)
        assertEquals(2, stats.pendingMatches)
        assertEquals(2, stats.activePlayers)
    }
}
