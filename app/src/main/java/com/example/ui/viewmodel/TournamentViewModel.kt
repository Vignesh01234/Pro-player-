package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CourtEntity
import com.example.data.local.FinancialSummary
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TeamEntity
import com.example.data.local.TeamStanding
import com.example.data.local.TransactionEntity
import com.example.data.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserRole {
    ADMIN,
    GENERAL_USER,
    LOGGED_OUT
}

enum class AppDestination {
    LOGIN,
    DASHBOARD,
    COURT_RENT,
    TOURNAMENT_LIVE,
    ROSTER,
    SCHEDULE,
    LIVE_SCORE,
    FINANCE
}

/**
 * Immutable state object exposing computed tournament statistics for the Dashboard UI.
 */
data class DashboardStatsState(
    val totalMatches: Int = 0,
    val totalMatchesPlayed: Int = 0,
    val pendingMatches: Int = 0,
    val liveMatchesCount: Int = 0,
    val scheduledMatchesCount: Int = 0,
    val activePlayers: Int = 0,
    val totalRegisteredPlayers: Int = 0,
    val confirmedTeams: Int = 0,
    val availableCourts: Int = 3,
    val completionFraction: Float = 0f,
    val completionPercent: Int = 0,
    val totalCollected: Double = 0.0,
    val pendingDues: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0
)

class TournamentViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _userRole = MutableStateFlow(UserRole.LOGGED_OUT)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppDestination.LOGIN)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    val players: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teams: StateFlow<List<TeamEntity>> = repository.allTeams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courts: StateFlow<List<CourtEntity>> = repository.allCourts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<FinancialSummary> = repository.financialSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    val dashboardStats: StateFlow<DashboardStatsState> = combine(
        players,
        teams,
        courts,
        matches,
        financialSummary
    ) { playerList, teamList, courtList, matchList, finance ->
        computeDashboardStats(
            players = playerList,
            teams = teamList,
            courts = courtList,
            matches = matchList,
            financialSummary = finance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStatsState())

    val standings: StateFlow<List<TeamStanding>> = combine(teams, matches) { teamList, matchList ->
        repository.calculateStandings(teamList, matchList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLiveMatchId = MutableStateFlow<Long?>(null)
    val selectedLiveMatchId: StateFlow<Long?> = _selectedLiveMatchId.asStateFlow()

    val activeLiveMatch: StateFlow<MatchEntity?> = combine(matches, _selectedLiveMatchId) { list, selectedId ->
        if (selectedId != null) {
            list.find { it.id == selectedId } ?: list.find { it.status == "Live" } ?: list.firstOrNull()
        } else {
            list.find { it.status == "Live" } ?: list.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _intervalAlertMessage = MutableStateFlow<String?>(null)
    val intervalAlertMessage: StateFlow<String?> = _intervalAlertMessage.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _monthlyCourtRent = MutableStateFlow(6000.0)
    val monthlyCourtRent: StateFlow<Double> = _monthlyCourtRent.asStateFlow()

    private val _selectedMonth = MutableStateFlow("November 2026")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun login(idInput: String, passwordInput: String): Boolean {
        val cleanId = idInput.trim()
        val cleanPass = passwordInput.trim()
        return when {
            cleanId == "admin" && cleanPass == "admin123" -> {
                _userRole.value = UserRole.ADMIN
                _currentDestination.value = AppDestination.DASHBOARD
                _statusBannerMessage.value = "Welcome Admin! Full modify permissions enabled."
                true
            }
            cleanId == "123" && cleanPass == "123" -> {
                _userRole.value = UserRole.GENERAL_USER
                _currentDestination.value = AppDestination.DASHBOARD
                _statusBannerMessage.value = "Logged in as General User. View-only access."
                true
            }
            else -> false
        }
    }

    fun logout() {
        _userRole.value = UserRole.LOGGED_OUT
        _currentDestination.value = AppDestination.LOGIN
        _statusBannerMessage.value = "Logged out successfully"
    }

    fun updateMonthlyCourtRent(newRent: Double) {
        val cleanRent = newRent.coerceAtLeast(0.0)
        _monthlyCourtRent.value = cleanRent
        _statusBannerMessage.value = "Monthly court rent updated"
    }

    fun updateSelectedMonth(month: String) {
        if (month.isNotBlank()) {
            _selectedMonth.value = month.trim()
        }
    }

    fun addSimplePlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val newCount = (players.value.size + 1).coerceAtLeast(1)
            val perPlayerShare = _monthlyCourtRent.value / newCount
            repository.addSimplePlayer(trimmed, perPlayerShare)
            _statusBannerMessage.value = "Added player: $trimmed"
        }
    }

    fun togglePlayerPaid(player: PlayerEntity) {
        viewModelScope.launch {
            val totalPlayers = players.value.size.coerceAtLeast(1)
            val perPlayerShare = _monthlyCourtRent.value / totalPlayers
            repository.togglePlayerPaidStatus(player, perPlayerShare)
            val nextStatus = when (player.paymentStatus) {
                "Paid" -> "Unpaid"
                "Unpaid" -> "Inactive"
                "Inactive" -> "Paid"
                else -> "Paid"
            }
            _statusBannerMessage.value = "${player.name} set to $nextStatus"
        }
    }

    fun setPlayerStatus(player: PlayerEntity, status: String) {
        viewModelScope.launch {
            val totalPlayers = players.value.size.coerceAtLeast(1)
            val perPlayerShare = _monthlyCourtRent.value / totalPlayers
            repository.setPlayerStatus(player, status, perPlayerShare)
            _statusBannerMessage.value = "${player.name} set to $status"
        }
    }

    fun renamePlayer(player: PlayerEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.renamePlayer(player, trimmed)
            _statusBannerMessage.value = "Renamed to $trimmed"
        }
    }

    fun markAllPlayersPaid(paid: Boolean) {
        viewModelScope.launch {
            val totalPlayers = players.value.size.coerceAtLeast(1)
            val perPlayerShare = _monthlyCourtRent.value / totalPlayers
            repository.setAllPlayersPaidStatus(paid, perPlayerShare)
            _statusBannerMessage.value = if (paid) "All players marked Paid" else "All players reset to Unpaid"
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun openMatchOnScoreboard(match: MatchEntity) {
        viewModelScope.launch {
            if (match.status == "Scheduled") {
                repository.startMatchLive(match)
            }
            _selectedLiveMatchId.value = match.id
            _currentDestination.value = AppDestination.LIVE_SCORE
        }
    }

    fun selectMatchForScoring(matchId: Long) {
        _selectedLiveMatchId.value = matchId
    }

    fun dismissIntervalAlert() {
        _intervalAlertMessage.value = null
    }

    fun dismissStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun scorePointForTeam(match: MatchEntity, teamNumber: Int) {
        viewModelScope.launch {
            val result = repository.scoreRallyPoint(match, teamNumber)
            if (result.triggeredIntervalAlert) {
                _intervalAlertMessage.value = result.statusBanner
            } else if (result.statusBanner != null) {
                _statusBannerMessage.value = result.statusBanner
            }
        }
    }

    fun undoLastPoint(match: MatchEntity) {
        viewModelScope.launch {
            repository.undoLastRallyPoint(match)
            _statusBannerMessage.value = "Last rally point undone"
        }
    }

    fun switchServiceManually(match: MatchEntity) {
        viewModelScope.launch {
            repository.switchServerSideManually(match)
        }
    }

    fun registerPlayer(
        name: String,
        phone: String,
        skillTier: String,
        category: String,
        registrationStatus: String,
        entryFeeTotal: Double,
        entryFeePaid: Double,
        paymentMode: String
    ) {
        viewModelScope.launch {
            repository.registerPlayerWithFee(
                name = name,
                phone = phone,
                skillTier = skillTier,
                category = category,
                registrationStatus = registrationStatus,
                entryFeeTotal = entryFeeTotal,
                entryFeePaid = entryFeePaid,
                paymentMode = paymentMode
            )
            _statusBannerMessage.value = "Player $name registered!"
        }
    }

    fun cyclePlayerStatus(player: PlayerEntity) {
        val next = when (player.registrationStatus) {
            "Confirmed" -> "Waitlisted"
            "Waitlisted" -> "Withdrawn"
            else -> "Confirmed"
        }
        viewModelScope.launch {
            repository.updatePlayerStatus(player, next)
        }
    }

    fun updatePlayer(
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
        viewModelScope.launch {
            repository.updatePlayerDetails(
                player = player,
                name = name,
                phone = phone,
                skillTier = skillTier,
                category = category,
                registrationStatus = registrationStatus,
                entryFeeTotal = entryFeeTotal,
                entryFeePaid = entryFeePaid,
                paymentMode = paymentMode
            )
            _statusBannerMessage.value = "Updated player: $name"
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
            _statusBannerMessage.value = "Removed player: ${player.name}"
        }
    }

    fun createDoublesTeam(
        customTeamName: String,
        category: String,
        player1: PlayerEntity,
        player2: PlayerEntity?,
        groupName: String,
        registrationStatus: String
    ) {
        viewModelScope.launch {
            repository.createTeam(
                customTeamName = customTeamName,
                category = category,
                player1 = player1,
                player2 = player2,
                groupName = groupName,
                registrationStatus = registrationStatus
            )
            _statusBannerMessage.value = "Team added to $groupName!"
        }
    }

    fun updateDoublesTeam(
        team: TeamEntity,
        customTeamName: String,
        category: String,
        player1: PlayerEntity,
        player2: PlayerEntity?,
        groupName: String,
        registrationStatus: String
    ) {
        viewModelScope.launch {
            repository.updateTeamDetails(
                team = team,
                customTeamName = customTeamName,
                category = category,
                player1 = player1,
                player2 = player2,
                groupName = groupName,
                registrationStatus = registrationStatus
            )
            _statusBannerMessage.value = "Updated team in $groupName!"
        }
    }

    fun deleteTeam(team: TeamEntity) {
        viewModelScope.launch {
            repository.deleteTeam(team)
            _statusBannerMessage.value = "Removed team: ${team.teamName}"
        }
    }

    fun cycleTeamStatus(team: TeamEntity) {
        val next = when (team.registrationStatus) {
            "Confirmed" -> "Waitlisted"
            "Waitlisted" -> "Withdrawn"
            else -> "Confirmed"
        }
        viewModelScope.launch {
            repository.updateTeamRegistrationStatus(team, next)
        }
    }

    /**
     * Convenience entry point to generate a full Round Robin schedule with back-to-back conflict avoidance.
     */
    fun generateRoundRobinFixtures(
        startHour: Int = 9,
        startMinute: Int = 0,
        slotDurationMin: Int = 35
    ) {
        generateFixtures(
            format = "Round Robin",
            startHour = startHour,
            startMinute = startMinute,
            slotDurationMin = slotDurationMin
        )
    }

    /**
     * Core ViewModel fixture generator: auto-creates matchups for Round Robin, Group + Knockout,
     * or Knockout formats while ensuring teams do not play back-to-back matches.
     */
    fun generateFixtures(
        format: String = "Round Robin",
        startHour: Int = 9,
        startMinute: Int = 0,
        slotDurationMin: Int = 35
    ) {
        viewModelScope.launch {
            val confirmedTeams = teams.value
                .filter { it.registrationStatus == "Confirmed" }
                .ifEmpty { repository.getConfirmedTeamsSnapshot() }

            if (confirmedTeams.size < 2) {
                _statusBannerMessage.value = "Need at least 2 Confirmed teams to generate fixtures"
                return@launch
            }

            val availableCourts = courts.value
                .filter { it.isAvailable }
                .ifEmpty { repository.getAvailableCourtsSnapshot() }

            val generatedMatches = buildTournamentFixtures(
                confirmedTeams = confirmedTeams,
                availableCourts = availableCourts,
                format = format,
                startHour = startHour,
                startMinute = startMinute,
                slotDurationMin = slotDurationMin
            )

            repository.replaceAllMatches(generatedMatches)
            _statusBannerMessage.value = "Generated ${generatedMatches.size} conflict-free matches ($format)"
        }
    }

    fun reassignMatchCourtAndTime(
        match: MatchEntity,
        courtName: String,
        scheduledTime: String,
        durationMin: Int
    ) {
        viewModelScope.launch {
            repository.updateMatchCourtAndTime(match, courtName, scheduledTime, durationMin)
            _statusBannerMessage.value = "Match #${match.matchNumber} moved to $courtName ($scheduledTime)"
        }
    }

    fun updateTransactionPayment(
        transaction: TransactionEntity,
        newAmountPaid: Double,
        paymentMode: String
    ) {
        viewModelScope.launch {
            repository.recordPaymentUpdate(transaction, newAmountPaid, paymentMode)
            _statusBannerMessage.value = "Updated payment for ${transaction.title}"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _statusBannerMessage.value = "Removed ledger entry: ${transaction.title}"
        }
    }

    fun addTransaction(
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
        viewModelScope.launch {
            repository.addExpenseOrEntryTransaction(
                title = title,
                type = type,
                expenseCategory = expenseCategory,
                playerPhone = playerPhone,
                teamName = teamName,
                amountExpected = amountExpected,
                amountPaid = amountPaid,
                paymentMode = paymentMode,
                notes = notes
            )
            _statusBannerMessage.value = "Saved ledger entry: $title"
        }
    }

    companion object {
        fun computeDashboardStats(
            players: List<PlayerEntity>,
            teams: List<TeamEntity>,
            courts: List<CourtEntity>,
            matches: List<MatchEntity>,
            financialSummary: FinancialSummary
        ): DashboardStatsState {
            val total = matches.size
            val played = matches.count { it.status == "Completed" }
            val live = matches.count { it.status == "Live" }
            val scheduled = matches.count { it.status == "Scheduled" }
            val pending = (total - played).coerceAtLeast(0)
            val activePlayersCount = players.count { it.registrationStatus != "Withdrawn" }
            val confirmedTeamsCount = teams.count { it.registrationStatus == "Confirmed" }
            val availCourts = courts.count { it.isAvailable }.let { if (it == 0) 3 else it }
            val fraction = if (total > 0) (played.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            val percent = (fraction * 100).toInt()

            return DashboardStatsState(
                totalMatches = total,
                totalMatchesPlayed = played,
                pendingMatches = pending,
                liveMatchesCount = live,
                scheduledMatchesCount = scheduled,
                activePlayers = activePlayersCount,
                totalRegisteredPlayers = players.size,
                confirmedTeams = confirmedTeamsCount,
                availableCourts = availCourts,
                completionFraction = fraction,
                completionPercent = percent,
                totalCollected = financialSummary.totalCollected,
                pendingDues = financialSummary.totalPendingDues,
                totalExpenses = financialSummary.totalExpenses,
                netBalance = financialSummary.netBalance
            )
        }

        /**
         * Generates Round Robin matchups using the Berger Circle Rotation method so rounds are
         * naturally balanced before applying back-to-back conflict elimination.
         */
        fun generateRoundRobinPairings(
            teams: List<TeamEntity>,
            defaultGroupLabel: String = "Open League"
        ): List<Triple<String, TeamEntity, TeamEntity>> {
            if (teams.size < 2) return emptyList()

            val rotationPool = teams.toMutableList<TeamEntity?>()
            if (rotationPool.size % 2 != 0) {
                rotationPool.add(null) // BYE slot for odd number of teams
            }

            val n = rotationPool.size
            val numRounds = n - 1
            val half = n / 2
            val pairings = mutableListOf<Triple<String, TeamEntity, TeamEntity>>()

            for (round in 0 until numRounds) {
                for (i in 0 until half) {
                    val t1 = rotationPool[i]
                    val t2 = rotationPool[n - 1 - i]
                    if (t1 != null && t2 != null) {
                        val grp = if (t1.groupName == t2.groupName && t1.groupName.isNotBlank()) {
                            t1.groupName
                        } else {
                            defaultGroupLabel
                        }
                        pairings.add(Triple(grp, t1, t2))
                    }
                }
                // Rotate all elements except index 0 clockwise
                val last = rotationPool.removeAt(n - 1)
                rotationPool.add(1, last)
            }

            return pairings
        }

        /**
         * Orders matchups so that no team plays back-to-back consecutive matches whenever
         * mathematically possible, combining degree/rest-interval heuristics with a 2-opt swap pass.
         */
        fun orderPairingsWithoutBackToBack(
            pairings: List<Triple<String, TeamEntity, TeamEntity>>
        ): List<Triple<String, TeamEntity, TeamEntity>> {
            if (pairings.size <= 1) return pairings

            val remaining = pairings.toMutableList()
            val ordered = mutableListOf<Triple<String, TeamEntity, TeamEntity>>()
            val lastPlayedStep = mutableMapOf<Long, Int>()
            var lastMatchTeams = emptySet<Long>()

            var step = 0
            while (remaining.isNotEmpty()) {
                // Count how many unscheduled matches each team still has left
                val remainingDegree = mutableMapOf<Long, Int>()
                for ((_, t1, t2) in remaining) {
                    remainingDegree[t1.id] = (remainingDegree[t1.id] ?: 0) + 1
                    remainingDegree[t2.id] = (remainingDegree[t2.id] ?: 0) + 1
                }

                // Filter candidates where neither team played in the immediately preceding match
                val conflictFreeIndices = remaining.indices.filter { idx ->
                    val (_, t1, t2) = remaining[idx]
                    t1.id !in lastMatchTeams && t2.id !in lastMatchTeams
                }

                val chosenIndex = if (conflictFreeIndices.isNotEmpty()) {
                    conflictFreeIndices.maxWithOrNull(
                        compareBy<Int> { idx ->
                            val (_, t1, t2) = remaining[idx]
                            val r1 = step - (lastPlayedStep[t1.id] ?: -10)
                            val r2 = step - (lastPlayedStep[t2.id] ?: -10)
                            minOf(r1, r2)
                        }.thenBy { idx ->
                            val (_, t1, t2) = remaining[idx]
                            (remainingDegree[t1.id] ?: 0) + (remainingDegree[t2.id] ?: 0)
                        }.thenBy { idx ->
                            val (_, t1, t2) = remaining[idx]
                            val r1 = step - (lastPlayedStep[t1.id] ?: -10)
                            val r2 = step - (lastPlayedStep[t2.id] ?: -10)
                            r1 + r2
                        }
                    ) ?: conflictFreeIndices.first()
                } else {
                    // Fallback when N <= 3 or tail end: pick candidate maximizing rest
                    remaining.indices.maxByOrNull { idx ->
                        val (_, t1, t2) = remaining[idx]
                        val r1 = step - (lastPlayedStep[t1.id] ?: -10)
                        val r2 = step - (lastPlayedStep[t2.id] ?: -10)
                        minOf(r1, r2) * 100 + r1 + r2
                    } ?: 0
                }

                val pick = remaining.removeAt(chosenIndex)
                ordered.add(pick)
                lastPlayedStep[pick.second.id] = step
                lastPlayedStep[pick.third.id] = step
                lastMatchTeams = setOf(pick.second.id, pick.third.id)
                step++
            }

            // 2-opt swap pass to eliminate any tail-end back-to-back conflicts
            resolveRemainingBackToBackConflicts(ordered)
            return ordered
        }

        private fun resolveRemainingBackToBackConflicts(
            schedule: MutableList<Triple<String, TeamEntity, TeamEntity>>
        ) {
            if (schedule.size < 3) return
            for (pass in 0 until 3) {
                var improved = false
                for (i in 1 until schedule.size) {
                    if (sharesAnyTeam(schedule[i - 1], schedule[i])) {
                        // Look for an index j to swap with schedule[i] that leaves both i and j conflict-free
                        for (j in schedule.indices) {
                            if (j == i || j == i - 1) continue
                            val candidateForI = schedule[j]
                            val candidateForJ = schedule[i]

                            val okAtI = !sharesAnyTeam(schedule[i - 1], candidateForI) &&
                                (i + 1 >= schedule.size || j == i + 1 || !sharesAnyTeam(candidateForI, schedule[i + 1]))
                            val okAtJ = (j == 0 || (j - 1 != i && !sharesAnyTeam(schedule[j - 1], candidateForJ))) &&
                                (j + 1 >= schedule.size || (j + 1 != i && !sharesAnyTeam(candidateForJ, schedule[j + 1])))

                            if (okAtI && okAtJ) {
                                val tmp = schedule[i]
                                schedule[i] = schedule[j]
                                schedule[j] = tmp
                                improved = true
                                break
                            }
                        }
                    }
                }
                if (!improved) break
            }
        }

        fun sharesAnyTeam(
            m1: Triple<String, TeamEntity, TeamEntity>,
            m2: Triple<String, TeamEntity, TeamEntity>
        ): Boolean {
            val a1 = m1.second.id
            val a2 = m1.third.id
            val b1 = m2.second.id
            val b2 = m2.third.id
            return a1 == b1 || a1 == b2 || a2 == b1 || a2 == b2
        }

        /**
         * Builds the full list of MatchEntity objects with court assignments, start times,
         * and back-to-back conflict avoidance.
         */
        fun buildTournamentFixtures(
            confirmedTeams: List<TeamEntity>,
            availableCourts: List<CourtEntity>,
            format: String = "Round Robin",
            startHour: Int = 9,
            startMinute: Int = 0,
            slotDurationMin: Int = 35
        ): List<MatchEntity> {
            if (confirmedTeams.size < 2) return emptyList()

            val courts = availableCourts.ifEmpty {
                listOf(CourtEntity(id = 1, courtName = "Court 1 • Arena Center"))
            }

            val rawPairings = mutableListOf<Triple<String, TeamEntity, TeamEntity>>()

            when (format) {
                "Round Robin" -> {
                    rawPairings.addAll(generateRoundRobinPairings(confirmedTeams, "Open League"))
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
                        rawPairings.addAll(generateRoundRobinPairings(grpTeams, grp))
                    }
                    if (rawPairings.isEmpty()) {
                        rawPairings.addAll(generateRoundRobinPairings(confirmedTeams, "Group A"))
                    }
                }
            }

            val orderedPairings = orderPairingsWithoutBackToBack(rawPairings)
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

            return generatedMatches
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

        fun provideFactory(repository: TournamentRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TournamentViewModel(repository) as T
                }
            }
        }
    }
}
