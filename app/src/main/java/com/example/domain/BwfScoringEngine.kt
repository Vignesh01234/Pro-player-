package com.example.domain

import com.example.data.local.MatchEntity

data class BwfRallyUpdateResult(
    val updatedMatch: MatchEntity,
    val triggeredIntervalAlert: Boolean,
    val gameJustWonBy: Int?, // 1 or 2
    val matchJustWonBy: Int?, // 1 or 2
    val statusBanner: String?
)

object BwfScoringEngine {

    /**
     * Standard BWF Scoring Rules:
     * - Best of 3 games
     * - First to 21 points wins a game
     * - At 20-all (Deuce), a side must gain a 2-point lead to win that game
     * - At 29-all (Golden Point), the side scoring the 30th point wins that game
     * - Service Court: If server's score is EVEN (0, 2, 4...), serve from RIGHT service court;
     *   if ODD (1, 3, 5...), serve from LEFT service court.
     * - Interval Alert: Triggered once per game when the leading score first reaches 11 points.
     */
    fun isGameWon(scoreA: Int, scoreB: Int): Boolean {
        if (scoreA >= 30) return true
        if (scoreA >= 21 && (scoreA - scoreB) >= 2) return true
        return false
    }

    fun getGamesWonCount(match: MatchEntity): Pair<Int, Int> {
        var t1Games = 0
        var t2Games = 0
        if (isGameWon(match.g1Team1, match.g1Team2)) t1Games++
        else if (isGameWon(match.g1Team2, match.g1Team1)) t2Games++

        if (isGameWon(match.g2Team1, match.g2Team2)) t1Games++
        else if (isGameWon(match.g2Team2, match.g2Team1)) t2Games++

        if (isGameWon(match.g3Team1, match.g3Team2)) t1Games++
        else if (isGameWon(match.g3Team2, match.g3Team1)) t2Games++

        return t1Games to t2Games
    }

    fun getCurrentGameScores(match: MatchEntity): Pair<Int, Int> {
        return when (match.currentGame) {
            1 -> match.g1Team1 to match.g1Team2
            2 -> match.g2Team1 to match.g2Team2
            else -> match.g3Team1 to match.g3Team2
        }
    }

    fun getServiceCourtBox(match: MatchEntity): String {
        val (s1, s2) = getCurrentGameScores(match)
        val serverScore = if (match.servingTeam == 1) s1 else s2
        return if (serverScore % 2 == 0) "Right Court (Even)" else "Left Court (Odd)"
    }

    fun getPointSituationLabel(match: MatchEntity): String? {
        if (match.status == "Completed") return "FINAL SCORE"
        val (s1, s2) = getCurrentGameScores(match)
        val (g1, g2) = getGamesWonCount(match)

        if (s1 == 29 && s2 == 29) return "GOLDEN POINT (29-ALL)"
        if (s1 >= 20 && s2 >= 20 && s1 == s2) return "DEUCE ($s1-ALL) • 2-PT LEAD NEEDED"

        // Check if Team 1 is on game/match point
        val t1NextWins = isGameWon(s1 + 1, s2)
        val t2NextWins = isGameWon(s2 + 1, s1)

        if (t1NextWins) {
            return if (g1 == 1) "MATCH POINT • ${match.team1Name.uppercase()}"
            else "GAME ${match.currentGame} POINT • ${match.team1Name.uppercase()}"
        }
        if (t2NextWins) {
            return if (g2 == 1) "MATCH POINT • ${match.team2Name.uppercase()}"
            else "GAME ${match.currentGame} POINT • ${match.team2Name.uppercase()}"
        }
        return null
    }

    fun scorePoint(match: MatchEntity, scoringTeam: Int): BwfRallyUpdateResult {
        if (match.status == "Completed") {
            return BwfRallyUpdateResult(match, false, null, null, "Match already completed")
        }

        val currentGame = match.currentGame
        var g1T1 = match.g1Team1
        var g1T2 = match.g1Team2
        var g2T1 = match.g2Team1
        var g2T2 = match.g2Team2
        var g3T1 = match.g3Team1
        var g3T2 = match.g3Team2

        var intG1 = match.intervalReachedG1
        var intG2 = match.intervalReachedG2
        var intG3 = match.intervalReachedG3
        var triggeredInterval = false

        when (currentGame) {
            1 -> {
                if (scoringTeam == 1) g1T1++ else g1T2++
                if (!intG1 && (g1T1 == 11 || g1T2 == 11) && maxOf(g1T1, g1T2) == 11) {
                    intG1 = true
                    triggeredInterval = true
                }
            }
            2 -> {
                if (scoringTeam == 1) g2T1++ else g2T2++
                if (!intG2 && (g2T1 == 11 || g2T2 == 11) && maxOf(g2T1, g2T2) == 11) {
                    intG2 = true
                    triggeredInterval = true
                }
            }
            else -> {
                if (scoringTeam == 1) g3T1++ else g3T2++
                if (!intG3 && (g3T1 == 11 || g3T2 == 11) && maxOf(g3T1, g3T2) == 11) {
                    intG3 = true
                    triggeredInterval = true
                }
            }
        }

        val newHistory = if (match.rallyHistory.isBlank()) {
            "$currentGame:$scoringTeam"
        } else {
            "${match.rallyHistory},$currentGame:$scoringTeam"
        }

        var tempMatch = match.copy(
            status = "Live",
            g1Team1 = g1T1,
            g1Team2 = g1T2,
            g2Team1 = g2T1,
            g2Team2 = g2T2,
            g3Team1 = g3T1,
            g3Team2 = g3T2,
            servingTeam = scoringTeam,
            intervalReachedG1 = intG1,
            intervalReachedG2 = intG2,
            intervalReachedG3 = intG3,
            rallyHistory = newHistory,
            updatedAt = System.currentTimeMillis()
        )

        val (curS1, curS2) = getCurrentGameScores(tempMatch)
        val t1WonGame = isGameWon(curS1, curS2)
        val t2WonGame = isGameWon(curS2, curS1)
        val gameWinner = when {
            t1WonGame -> 1
            t2WonGame -> 2
            else -> null
        }

        var matchWinner: Int? = null
        var banner: String? = null

        if (triggeredInterval) {
            val leader = if (scoringTeam == 1) match.team1Name else match.team2Name
            banner = "11-POINT INTERVAL! 60s Break & Coaching ($leader leads Game $currentGame)"
        }

        if (gameWinner != null) {
            val (games1, games2) = getGamesWonCount(tempMatch)
            if (games1 >= 2) {
                matchWinner = 1
                tempMatch = tempMatch.copy(
                    status = "Completed",
                    winnerTeamId = match.team1Id
                )
                banner = "${match.team1Name} wins the match ($games1-$games2)!"
            } else if (games2 >= 2) {
                matchWinner = 2
                tempMatch = tempMatch.copy(
                    status = "Completed",
                    winnerTeamId = match.team2Id
                )
                banner = "${match.team2Name} wins the match ($games2-$games1)!"
            } else {
                val nextGame = (currentGame + 1).coerceAtMost(3)
                tempMatch = tempMatch.copy(
                    currentGame = nextGame,
                    servingTeam = gameWinner
                )
                val winnerName = if (gameWinner == 1) match.team1Name else match.team2Name
                banner = "Game $currentGame won by $winnerName! Starting Game $nextGame."
            }
        }

        return BwfRallyUpdateResult(
            updatedMatch = tempMatch,
            triggeredIntervalAlert = triggeredInterval,
            gameJustWonBy = gameWinner,
            matchJustWonBy = matchWinner,
            statusBanner = banner
        )
    }

    fun undoLastPoint(match: MatchEntity): MatchEntity {
        if (match.rallyHistory.isBlank()) return match
        val tokens = match.rallyHistory.split(",").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return match

        val lastEntry = tokens.last()
        val parts = lastEntry.split(":")
        if (parts.size != 2) return match

        val gameIndex = parts[0].toIntOrNull() ?: match.currentGame
        val teamScored = parts[1].toIntOrNull() ?: 1

        var g1T1 = match.g1Team1
        var g1T2 = match.g1Team2
        var g2T1 = match.g2Team1
        var g2T2 = match.g2Team2
        var g3T1 = match.g3Team1
        var g3T2 = match.g3Team2

        when (gameIndex) {
            1 -> if (teamScored == 1) g1T1 = (g1T1 - 1).coerceAtLeast(0) else g1T2 = (g1T2 - 1).coerceAtLeast(0)
            2 -> if (teamScored == 1) g2T1 = (g2T1 - 1).coerceAtLeast(0) else g2T2 = (g2T2 - 1).coerceAtLeast(0)
            else -> if (teamScored == 1) g3T1 = (g3T1 - 1).coerceAtLeast(0) else g3T2 = (g3T2 - 1).coerceAtLeast(0)
        }

        val remainingTokens = tokens.dropLast(1)
        val prevServingTeam = if (remainingTokens.isNotEmpty()) {
            remainingTokens.last().split(":").getOrNull(1)?.toIntOrNull() ?: 1
        } else 1

        return match.copy(
            status = "Live",
            currentGame = gameIndex,
            g1Team1 = g1T1,
            g1Team2 = g1T2,
            g2Team1 = g2T1,
            g2Team2 = g2T2,
            g3Team1 = g3T1,
            g3Team2 = g3T2,
            servingTeam = prevServingTeam,
            winnerTeamId = null,
            rallyHistory = remainingTokens.joinToString(","),
            updatedAt = System.currentTimeMillis()
        )
    }
}
