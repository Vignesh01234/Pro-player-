package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsTennis
import com.example.ui.viewmodel.UserRole
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerEntity
import com.example.ui.theme.CourtGreenLight
import com.example.ui.theme.CourtGreenPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShuttleGoldAccent
import com.example.util.ExportShareHelper
import kotlin.math.roundToInt

// Status Color Tokens
private val PaidGreen = Color(0xFF007A53)
private val PaidGreenLight = Color(0xFF4EE8B2)
private val UnpaidCrimson = Color(0xFFFF5252)
private val InactiveGray = Color(0xFF8E9995)
private val PendingAmber = Color(0xFFFFB74D)

private val MONTHS = listOf(
    "January 2026", "February 2026", "March 2026", "April 2026",
    "May 2026", "June 2026", "July 2026", "August 2026",
    "September 2026", "October 2026", "November 2026", "December 2026"
)

@Composable
fun CourtRentScreen(
    userRole: UserRole,
    players: List<PlayerEntity>,
    monthlyCourtRent: Double,
    selectedMonth: String,
    onBack: () -> Unit,
    onUpdateMonthlyRent: (Double) -> Unit,
    onUpdateSelectedMonth: (String) -> Unit,
    onAddPlayer: (String) -> Unit,
    onTogglePlayerPaid: (PlayerEntity) -> Unit,
    onSetPlayerStatus: (PlayerEntity, String) -> Unit,
    onRenamePlayer: (PlayerEntity, String) -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit,
    onMarkAllPaid: (Boolean) -> Unit
) {
    var newPlayerName by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("All") } // "All", "Paid", "Unpaid", "Inactive"
    var showEditRentDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var historyPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    val totalPlayers = players.size
    val paidCount = players.count { it.paymentStatus == "Paid" }
    val unpaidCount = players.count { it.paymentStatus == "Unpaid" || (it.paymentStatus != "Paid" && it.paymentStatus != "Inactive") }
    val inactiveCount = players.count { it.paymentStatus == "Inactive" }

    // Active players sharing the rent = Paid + Unpaid (Inactive players do not count or share rent)
    val activePlayersCount = (paidCount + unpaidCount)
    val perPlayerShare = if (activePlayersCount > 0) {
        (monthlyCourtRent / activePlayersCount).roundToInt().toDouble()
    } else if (totalPlayers > 0) {
        (monthlyCourtRent / totalPlayers).roundToInt().toDouble()
    } else {
        0.0
    }

    val totalCollected = paidCount * perPlayerShare
    val totalPending = (monthlyCourtRent - totalCollected).coerceAtLeast(0.0)

    val filteredPlayers = remember(players, filterMode) {
        when (filterMode) {
            "Paid" -> players.filter { it.paymentStatus == "Paid" }
            "Unpaid" -> players.filter { it.paymentStatus == "Unpaid" || (it.paymentStatus != "Paid" && it.paymentStatus != "Inactive") }
            "Inactive" -> players.filter { it.paymentStatus == "Inactive" }
            else -> players
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("court_rent_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Navigation & Role Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("rent_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard", tint = ShuttleGoldAccent)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "COURT RENT MANAGEMENT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShuttleGoldAccent
                            )
                            Text(
                                text = if (userRole == UserRole.ADMIN) "Admin Mode (Modify Enabled)" else "General User (View Only)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (userRole == UserRole.ADMIN) CourtGreenPrimary else InactiveGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (userRole == UserRole.ADMIN) "ADMIN" else "VIEWER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. Monthly Court Rent Summary Card
            item {
                MonthlyRentSummaryCard(
                    userRole = userRole,
                    selectedMonth = selectedMonth,
                    monthlyCourtRent = monthlyCourtRent,
                    perPlayerShare = perPlayerShare,
                    totalPlayers = totalPlayers,
                    paidCount = paidCount,
                    unpaidCount = unpaidCount,
                    inactiveCount = inactiveCount,
                    totalCollected = totalCollected,
                    totalPending = totalPending,
                    onCycleMonth = {
                        val idx = MONTHS.indexOf(selectedMonth)
                        val nextIdx = if (idx == -1) 0 else (idx + 1) % MONTHS.size
                        onUpdateSelectedMonth(MONTHS[nextIdx])
                    },
                    onEditRentClick = { showEditRentDialog = true },
                    onMarkAllPaid = { onMarkAllPaid(true) },
                    onResetAllUnpaid = { onMarkAllPaid(false) }
                )
            }

            // 2. Quick Add Player Input Card (Admin only)
            if (userRole == UserRole.ADMIN) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, CourtGreenPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ADD PLAYER",
                            style = MaterialTheme.typography.labelLarge,
                            color = ShuttleGoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlayerName,
                                onValueChange = { newPlayerName = it },
                                placeholder = { Text("Enter player name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_name_input")
                            )
                            Button(
                                onClick = {
                                    if (newPlayerName.isNotBlank()) {
                                        onAddPlayer(newPlayerName)
                                        newPlayerName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShuttleGoldAccent,
                                    contentColor = Color(0xFF261900)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("add_player_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Player",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

            // 3. 3-Category Filter Section: All, 1: Paid, 2: Unpaid, 3: Inactive (Equal Size Design)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FILTER PLAYERS",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalPlayers Total Members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Row 1: All & 1: Paid (Equal 50% width)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryEqualFilterCard(
                            key = "All",
                            title = "ALL",
                            count = totalPlayers,
                            activeColor = CourtGreenPrimary,
                            isSelected = filterMode == "All",
                            onClick = { filterMode = "All" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryEqualFilterCard(
                            key = "Paid",
                            title = "1: PAID",
                            count = paidCount,
                            activeColor = PaidGreen,
                            isSelected = filterMode == "Paid",
                            onClick = { filterMode = "Paid" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: 2: Unpaid & 3: Inactive (Equal 50% width)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryEqualFilterCard(
                            key = "Unpaid",
                            title = "2: UNPAID",
                            count = unpaidCount,
                            activeColor = UnpaidCrimson,
                            isSelected = filterMode == "Unpaid",
                            onClick = { filterMode = "Unpaid" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryEqualFilterCard(
                            key = "Inactive",
                            title = "3: INACTIVE",
                            count = inactiveCount,
                            activeColor = InactiveGray,
                            isSelected = filterMode == "Inactive",
                            onClick = { filterMode = "Inactive" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Empty State or Player Rows
            if (filteredPlayers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "No Players",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (players.isEmpty()) {
                                    "No players added yet. Enter a player name above and tap Add."
                                } else {
                                    "No $filterMode players found."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredPlayers, key = { it.id }) { player ->
                    PlayerRentRowCard(
                        userRole = userRole,
                        player = player,
                        perPlayerShare = perPlayerShare,
                        onTogglePaid = { if (userRole == UserRole.ADMIN) onTogglePlayerPaid(player) },
                        onSelectStatus = { newStatus -> if (userRole == UserRole.ADMIN) onSetPlayerStatus(player, newStatus) },
                        onEditName = { if (userRole == UserRole.ADMIN) editingPlayer = player },
                        onDelete = { if (userRole == UserRole.ADMIN) onDeletePlayer(player) },
                        onInspectHistory = { historyPlayer = player }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Individual Player Month-Wise History Dialog
    val playerForHistory = historyPlayer
    if (playerForHistory != null) {
        PlayerMonthWiseHistoryDialog(
            player = playerForHistory,
            currentMonth = selectedMonth,
            perPlayerShare = perPlayerShare,
            onDismiss = { historyPlayer = null }
        )
    }

    // Edit Monthly Court Rent Dialog (Admin only)
    if (showEditRentDialog && userRole == UserRole.ADMIN) {
        EditMonthlyRentDialog(
            currentRent = monthlyCourtRent,
            currentMonth = selectedMonth,
            onDismiss = { showEditRentDialog = false },
            onSave = { newRent, newMonth ->
                onUpdateMonthlyRent(newRent)
                onUpdateSelectedMonth(newMonth)
                showEditRentDialog = false
            }
        )
    }

    // Rename Player Dialog (Admin only)
    val playerToEdit = editingPlayer
    if (playerToEdit != null && userRole == UserRole.ADMIN) {
        RenamePlayerDialog(
            player = playerToEdit,
            onDismiss = { editingPlayer = null },
            onSave = { updatedName ->
                onRenamePlayer(playerToEdit, updatedName)
                editingPlayer = null
            }
        )
    }
}

@Composable
private fun MonthlyRentSummaryCard(
    userRole: UserRole,
    selectedMonth: String,
    monthlyCourtRent: Double,
    perPlayerShare: Double,
    totalPlayers: Int,
    paidCount: Int,
    unpaidCount: Int,
    inactiveCount: Int,
    totalCollected: Double,
    totalPending: Double,
    onCycleMonth: () -> Unit,
    onEditRentClick: () -> Unit,
    onMarkAllPaid: () -> Unit,
    onResetAllUnpaid: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, ShuttleGoldAccent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_rent_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: Month pill + Edit Rent button (Admin only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { if (userRole == UserRole.ADMIN) onCycleMonth() },
                    color = CourtGreenPrimary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("month_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = "Month",
                            tint = ShuttleGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedMonth.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = ShuttleGoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (userRole == UserRole.ADMIN) {
                    OutlinedButton(
                        onClick = onEditRentClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ShuttleGoldAccent.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_rent_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Monthly Rent",
                            tint = ShuttleGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Rent",
                            color = ShuttleGoldAccent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "View Only",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Main Rent & Per-Player Split Row
            val activePlayersCount = paidCount + unpaidCount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "MONTHLY COURT RENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ExportShareHelper.formatRupeeAnnotated(monthlyCourtRent),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ShuttleGoldAccent
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PER ACTIVE ($activePlayersCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ExportShareHelper.formatRupeeAnnotated(perPlayerShare),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = CourtGreenLight
                        )
                    }
                }
            }

            // 3 Categories Status Strip: 1: Paid • 2: Unpaid • 3: Inactive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatMiniPill(
                    label = "1: PAID",
                    valueText = "$paidCount",
                    accentColor = PaidGreenLight,
                    modifier = Modifier.weight(1f)
                )
                StatMiniPill(
                    label = "2: UNPAID",
                    valueText = "$unpaidCount",
                    accentColor = if (unpaidCount > 0) UnpaidCrimson else PaidGreenLight,
                    modifier = Modifier.weight(1f)
                )
                StatMiniPill(
                    label = "3: INACTIVE",
                    valueText = "$inactiveCount",
                    accentColor = InactiveGray,
                    modifier = Modifier.weight(1f)
                )
                StatMiniPill(
                    label = "COLLECTED",
                    valueAnnotated = ExportShareHelper.formatRupeeAnnotated(totalCollected),
                    accentColor = PaidGreenLight,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Quick Actions: Mark All Paid / Reset All Unpaid (Admin only)
            if (userRole == UserRole.ADMIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetAllUnpaid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_all_unpaid_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Unpaid",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Unpaid")
                    }

                    Button(
                        onClick = onMarkAllPaid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CourtGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mark_all_paid_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark All Paid",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Paid", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    label: String,
    valueText: String? = null,
    valueAnnotated: androidx.compose.ui.text.AnnotatedString? = null,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (valueAnnotated != null) {
                Text(
                    text = valueAnnotated,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
            } else {
                Text(
                    text = valueText ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryEqualFilterCard(
    key: String,
    title: String,
    count: Int,
    activeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .height(52.dp)
            .testTag("filter_chip_${key.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else activeColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else activeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerRentRowCard(
    userRole: UserRole,
    player: PlayerEntity,
    perPlayerShare: Double,
    onTogglePaid: () -> Unit,
    onSelectStatus: (String) -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
    onInspectHistory: () -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }

    val status = when (player.paymentStatus) {
        "Paid" -> "Paid"
        "Inactive" -> "Inactive"
        else -> "Unpaid"
    }

    val (badgeBgColor, badgeTextColor, badgeIcon, badgeLabel) = when (status) {
        "Paid" -> Quadruple(
            PaidGreen,
            Color.White,
            Icons.Default.CheckCircle,
            "1: PAID"
        )
        "Inactive" -> Quadruple(
            InactiveGray.copy(alpha = 0.25f),
            InactiveGray,
            Icons.Default.NotInterested,
            "3: INACTIVE"
        )
        else -> Quadruple(
            UnpaidCrimson.copy(alpha = 0.2f),
            UnpaidCrimson,
            Icons.Default.RadioButtonUnchecked,
            "2: UNPAID"
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (status) {
                "Paid" -> PaidGreenLight.copy(alpha = 0.45f)
                "Inactive" -> InactiveGray.copy(alpha = 0.35f)
                else -> UnpaidCrimson.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_row_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Avatar + Name + Category Share Amount (Click to view Month History)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInspectHistory() }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                "Paid" -> PaidGreen.copy(alpha = 0.25f)
                                "Inactive" -> InactiveGray.copy(alpha = 0.15f)
                                else -> UnpaidCrimson.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = when (status) {
                            "Paid" -> PaidGreenLight
                            "Inactive" -> InactiveGray
                            else -> UnpaidCrimson
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (status == "Inactive") {
                            ExportShareHelper.withRupeeSpan("Inactive member (No rent)")
                        } else {
                            ExportShareHelper.withRupeeSpan(
                                "Share: ₹${ExportShareHelper.formatNumberIndian(perPlayerShare)}"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status == "Inactive") InactiveGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Center-Right: Quick Category Button with FIXED UNIFORM SIZE (116.dp width, 48.dp height)
            Box {
                Button(
                    onClick = onTogglePaid,
                    enabled = userRole == UserRole.ADMIN,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = badgeBgColor,
                        contentColor = badgeTextColor,
                        disabledContainerColor = badgeBgColor.copy(alpha = 0.85f),
                        disabledContentColor = badgeTextColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .width(116.dp)
                        .height(48.dp)
                        .testTag("toggle_paid_btn_${player.id}")
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = badgeLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                if (userRole == UserRole.ADMIN) {
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("1: Paid", fontWeight = FontWeight.Bold, color = PaidGreen) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaidGreen) },
                            onClick = {
                                onSelectStatus("Paid")
                                showCategoryMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("2: Unpaid", fontWeight = FontWeight.Bold, color = UnpaidCrimson) },
                            leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = UnpaidCrimson) },
                            onClick = {
                                onSelectStatus("Unpaid")
                                showCategoryMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("3: Inactive", fontWeight = FontWeight.Bold, color = InactiveGray) },
                            leadingIcon = { Icon(Icons.Default.NotInterested, contentDescription = null, tint = InactiveGray) },
                            onClick = {
                                onSelectStatus("Inactive")
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Quick Category Picker Menu Trigger (Admin) or Month History (Viewer)
            if (userRole == UserRole.ADMIN) {
                IconButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("category_menu_btn_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = "Select category for ${player.name}",
                        tint = ShuttleGoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEditName,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_player_btn_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${player.name}",
                        tint = ShuttleGoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_player_btn_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ${player.name}",
                        tint = UnpaidCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onInspectHistory,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("history_player_btn_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View month history for ${player.name}",
                        tint = ShuttleGoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun EditMonthlyRentDialog(
    currentRent: Double,
    currentMonth: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var rentInput by remember { mutableStateOf(currentRent.roundToInt().toString()) }
    var monthInput by remember { mutableStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Monthly Court Rent",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monthInput,
                    onValueChange = { monthInput = it },
                    label = { Text("Month (e.g. November 2026)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_month_input")
                )
                OutlinedTextField(
                    value = rentInput,
                    onValueChange = { rentInput = it },
                    label = {
                        Text(
                            ExportShareHelper.withRupeeSpan("Monthly Court Rent (₹)")
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_rent_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedRent = rentInput.toDoubleOrNull() ?: currentRent
                    onSave(parsedRent, monthInput.ifBlank { currentMonth })
                },
                colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                modifier = Modifier.testTag("save_rent_btn")
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenamePlayerDialog(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember(player.id) { mutableStateOf(player.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Player Name",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_player_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                modifier = Modifier.testTag("confirm_rename_btn")
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
