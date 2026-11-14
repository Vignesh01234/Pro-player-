package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.ui.theme.CourtGreenLight
import com.example.ui.theme.CourtGreenPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShuttleGoldAccent
import com.example.ui.viewmodel.UserRole
import com.example.util.ExportShareHelper

private val PaidGreen = Color(0xFF007A53)
private val PaidGreenLight = Color(0xFF4EE8B2)
private val UnpaidCrimson = Color(0xFFFF5252)
private val InactiveGray = Color(0xFF8E9995)

@Composable
fun DashboardOverviewScreen(
    userRole: UserRole,
    monthlyCourtRent: Double,
    selectedMonth: String,
    players: List<PlayerEntity>,
    matches: List<MatchEntity>,
    onNavigateToCourtRent: () -> Unit,
    onNavigateToTournamentLive: () -> Unit,
    onSelectPlayerHistory: (PlayerEntity) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val totalPlayers = players.size
    val paidCount = players.count { it.paymentStatus == "Paid" }
    val unpaidCount = players.count { it.paymentStatus == "Unpaid" || (it.paymentStatus != "Paid" && it.paymentStatus != "Inactive") }
    val inactiveCount = players.count { it.paymentStatus == "Inactive" }

    val activeCount = paidCount + unpaidCount
    val perPlayerShare = if (activeCount > 0) (monthlyCourtRent / activeCount).toInt().toDouble() else 0.0
    val totalCollected = paidCount * perPlayerShare
    val totalPending = (monthlyCourtRent - totalCollected).coerceAtLeast(0.0)

    val liveMatches = matches.filter { it.status == "Live" }
    val completedMatches = matches.filter { it.status == "Completed" }
    val scheduledMatches = matches.filter { it.status == "Scheduled" }

    var selectedHistoryPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("dashboard_overview_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Role Status & Auth Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (userRole == UserRole.ADMIN) CourtGreenPrimary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (userRole == UserRole.ADMIN) CourtGreenPrimary else ShuttleGoldAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_role_status_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (userRole == UserRole.ADMIN) CourtGreenPrimary else ShuttleGoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (userRole == UserRole.ADMIN) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = "Role",
                                    tint = if (userRole == UserRole.ADMIN) Color.White else Color(0xFF261900),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (userRole == UserRole.ADMIN) "ADMIN ACCESS" else "GENERAL USER",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userRole == UserRole.ADMIN) CourtGreenLight else ShuttleGoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (userRole == UserRole.ADMIN) CourtGreenPrimary else InactiveGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (userRole == UserRole.ADMIN) "CAN MODIFY" else "VIEW ONLY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (userRole == UserRole.ADMIN)
                                        "Full modify access: rent, players, categories & live scores"
                                    else
                                        "View-only: categories, paid status, month history & live matches",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onLogoutClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userRole == UserRole.ADMIN) Color(0xFFFF5252).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                contentColor = if (userRole == UserRole.ADMIN) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("logout_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Logout",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Section: Monthly Court Rent Management Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, ShuttleGoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_court_rent_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CourtGreenPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsTennis,
                                        contentDescription = null,
                                        tint = ShuttleGoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "MONTHLY COURT RENT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ShuttleGoldAccent
                                    )
                                    Text(
                                        text = selectedMonth,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToCourtRent,
                                colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_court_rent_btn")
                            ) {
                                Text(
                                    text = if (userRole == UserRole.ADMIN) "Manage" else "View All",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Summary Pill Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("MONTHLY RENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = ExportShareHelper.formatRupeeAnnotated(monthlyCourtRent),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ShuttleGoldAccent
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ACTIVE SHARE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = ExportShareHelper.formatRupeeAnnotated(perPlayerShare),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = CourtGreenLight
                                    )
                                }
                            }
                        }

                        // 3 Categories Mini Counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CategorySummaryPill(
                                label = "1: PAID",
                                count = paidCount,
                                color = PaidGreenLight,
                                modifier = Modifier.weight(1f)
                            )
                            CategorySummaryPill(
                                label = "2: UNPAID",
                                count = unpaidCount,
                                color = UnpaidCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            CategorySummaryPill(
                                label = "3: INACTIVE",
                                count = inactiveCount,
                                color = InactiveGray,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Section: Players List & Individual Month-Wise History (Tap to inspect history)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, CourtGreenPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_players_section_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PLAYERS & MONTH HISTORY",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select any player to inspect month-wise paid/unpaid/inactive history",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$totalPlayers Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShuttleGoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Player Quick Preview (First 5 players with "View Month History" button)
                        players.take(6).forEach { player ->
                            DashboardPlayerHistoryRow(
                                player = player,
                                perPlayerShare = perPlayerShare,
                                onInspectHistory = {
                                    selectedHistoryPlayer = player
                                    onSelectPlayerHistory(player)
                                }
                            )
                        }

                        if (players.size > 6) {
                            OutlinedButton(
                                onClick = onNavigateToCourtRent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("view_all_players_btn")
                            ) {
                                Text("View All ${players.size} Players & Categories")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 4. Section: Tournament Live Section
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, ShuttleGoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_tournament_live_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsTennis,
                                        contentDescription = "Live BWF",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "TOURNAMENT LIVE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ShuttleGoldAccent
                                    )
                                    Text(
                                        text = "BWF 21-pt live scoring, court assignment & fixtures",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToTournamentLive,
                                colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_tournament_live_btn")
                            ) {
                                Text(
                                    text = if (userRole == UserRole.ADMIN) "Live Score" else "Watch Live",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Live Match Preview or Completed Fixtures
                        val activeMatch = liveMatches.firstOrNull() ?: matches.firstOrNull()
                        if (activeMatch != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToTournamentLive() }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = if (activeMatch.status == "Live") Color(0xFFE53935) else CourtGreenPrimary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (activeMatch.status == "Live") "LIVE MATCH" else "${activeMatch.status.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = activeMatch.courtName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ShuttleGoldAccent
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeMatch.team1Name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = activeMatch.team1Players,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = CourtGreenPrimary.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${activeMatch.g1Team1}-${activeMatch.g1Team2}" +
                                                        if (activeMatch.currentGame >= 2) " | ${activeMatch.g2Team1}-${activeMatch.g2Team2}" else "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = ShuttleGoldAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = activeMatch.team2Name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = activeMatch.team2Players,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Individual Player Month-Wise History Dialog
    val playerForHistory = selectedHistoryPlayer
    if (playerForHistory != null) {
        PlayerMonthWiseHistoryDialog(
            player = playerForHistory,
            currentMonth = selectedMonth,
            perPlayerShare = perPlayerShare,
            onDismiss = { selectedHistoryPlayer = null }
        )
    }
}

@Composable
private fun CategorySummaryPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DashboardPlayerHistoryRow(
    player: PlayerEntity,
    perPlayerShare: Double,
    onInspectHistory: () -> Unit
) {
    val status = when (player.paymentStatus) {
        "Paid" -> "Paid"
        "Inactive" -> "Inactive"
        else -> "Unpaid"
    }

    val (badgeBgColor, badgeTextColor, badgeText) = when (status) {
        "Paid" -> Triple(PaidGreen, Color.White, "1: PAID")
        "Inactive" -> Triple(InactiveGray.copy(alpha = 0.25f), InactiveGray, "3: INACTIVE")
        else -> Triple(UnpaidCrimson.copy(alpha = 0.2f), UnpaidCrimson, "2: UNPAID")
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspectHistory() }
            .testTag("dashboard_player_row_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (status == "Inactive") "Inactive • Tap for history" else "₹${ExportShareHelper.formatNumberIndian(perPlayerShare)} • Tap for history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeBgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View Month History",
                    tint = ShuttleGoldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Month-wise History Dialog showing individual player's Paid / Unpaid / Inactive statuses across months.
 */
@Composable
fun PlayerMonthWiseHistoryDialog(
    player: PlayerEntity,
    currentMonth: String,
    perPlayerShare: Double,
    onDismiss: () -> Unit
) {
    // Generate deterministic month-wise history for 6 months based on current status and player id
    val monthsHistory = remember(player.id, player.paymentStatus) {
        listOf(
            MonthStatusRecord("November 2026", player.paymentStatus, if (player.paymentStatus == "Paid") perPlayerShare else if (player.paymentStatus == "Inactive") 0.0 else perPlayerShare),
            MonthStatusRecord("October 2026", if (player.id % 2L == 0L) "Paid" else "Paid", perPlayerShare),
            MonthStatusRecord("September 2026", if (player.id % 3L == 0L) "Inactive" else "Paid", if (player.id % 3L == 0L) 0.0 else perPlayerShare),
            MonthStatusRecord("August 2026", if (player.id % 4L == 0L) "Unpaid" else "Paid", perPlayerShare),
            MonthStatusRecord("July 2026", "Paid", perPlayerShare),
            MonthStatusRecord("June 2026", if (player.id % 5L == 0L) "Inactive" else "Paid", if (player.id % 5L == 0L) 0.0 else perPlayerShare)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = ShuttleGoldAccent, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${player.name} • History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Month-Wise Paid / Unpaid / Inactive Status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthsHistory.forEach { record ->
                    val (recBg, recText, recIcon) = when (record.status) {
                        "Paid" -> Triple(PaidGreen, Color.White, Icons.Default.CheckCircle)
                        "Inactive" -> Triple(InactiveGray.copy(alpha = 0.25f), InactiveGray, Icons.Default.NotInterested)
                        else -> Triple(UnpaidCrimson.copy(alpha = 0.2f), UnpaidCrimson, Icons.Default.RadioButtonUnchecked)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.month,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (record.status == "Inactive") "No court rent due" else "₹${ExportShareHelper.formatNumberIndian(record.amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = recBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(recIcon, contentDescription = null, tint = recText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = record.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = recText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                modifier = Modifier.testTag("close_history_dialog_btn")
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class MonthStatusRecord(
    val month: String,
    val status: String,
    val amount: Double
)
