package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.TournamentRepository
import com.example.ui.screens.CourtRentScreen
import com.example.ui.screens.DashboardOverviewScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.TournamentLiveScreen
import com.example.ui.theme.CourtGreenPrimary
import com.example.ui.theme.ShuttleGoldAccent
import com.example.ui.theme.ShuttleProTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.TournamentViewModel
import com.example.ui.viewmodel.UserRole
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TournamentRepository(database.tournamentDao())

        setContent {
            ShuttleProTheme(darkTheme = true) {
                val tournamentViewModel: TournamentViewModel = viewModel(
                    factory = TournamentViewModel.provideFactory(repository)
                )
                ShuttleProApp(viewModel = tournamentViewModel)
            }
        }
    }
}

@Composable
fun ShuttleProApp(viewModel: TournamentViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val activeMatch by viewModel.activeLiveMatch.collectAsStateWithLifecycle()
    val monthlyCourtRent by viewModel.monthlyCourtRent.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(2800)
            viewModel.dismissStatusBanner()
        }
    }

    // Handle system back button to return to Dashboard from sub-screens
    BackHandler(enabled = currentDestination != AppDestination.DASHBOARD && currentDestination != AppDestination.LOGIN) {
        viewModel.navigateTo(AppDestination.DASHBOARD)
    }

    // If logged out or on Login screen, show full-screen Login
    if (userRole == UserRole.LOGGED_OUT || currentDestination == AppDestination.LOGIN) {
        LoginScreen(
            onLogin = { id, pass -> viewModel.login(id, pass) }
        )
        return
    }

    val paidCount = players.count { it.paymentStatus == "Paid" }
    val unpaidCount = players.count { it.paymentStatus == "Unpaid" || (it.paymentStatus != "Paid" && it.paymentStatus != "Inactive") }
    val inactiveCount = players.count { it.paymentStatus == "Inactive" }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentDestination == AppDestination.DASHBOARD,
                    onClick = { viewModel.navigateTo(AppDestination.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF002114),
                        selectedTextColor = ShuttleGoldAccent,
                        indicatorColor = CourtGreenPrimary
                    ),
                    modifier = Modifier.testTag("nav_dashboard_btn")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.COURT_RENT,
                    onClick = { viewModel.navigateTo(AppDestination.COURT_RENT) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Court Rent") },
                    label = { Text("Court Rent") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF002114),
                        selectedTextColor = ShuttleGoldAccent,
                        indicatorColor = CourtGreenPrimary
                    ),
                    modifier = Modifier.testTag("nav_court_rent_btn")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.TOURNAMENT_LIVE,
                    onClick = { viewModel.navigateTo(AppDestination.TOURNAMENT_LIVE) },
                    icon = { Icon(Icons.Default.SportsTennis, contentDescription = "Live BWF") },
                    label = { Text("Live BWF") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF002114),
                        selectedTextColor = ShuttleGoldAccent,
                        indicatorColor = CourtGreenPrimary
                    ),
                    modifier = Modifier.testTag("nav_live_bwf_btn")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global App Status Banner
            TopAppHeader(
                userRole = userRole,
                paidCount = paidCount,
                unpaidCount = unpaidCount,
                inactiveCount = inactiveCount,
                onLogout = { viewModel.logout() }
            )

            // Transient Feedback Banner
            AnimatedVisibility(visible = statusBanner != null) {
                Surface(
                    color = CourtGreenPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.dismissStatusBanner() }
                ) {
                    Text(
                        text = statusBanner ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> DashboardOverviewScreen(
                        userRole = userRole,
                        monthlyCourtRent = monthlyCourtRent,
                        selectedMonth = selectedMonth,
                        players = players,
                        matches = matches,
                        onNavigateToCourtRent = { viewModel.navigateTo(AppDestination.COURT_RENT) },
                        onNavigateToTournamentLive = { viewModel.navigateTo(AppDestination.TOURNAMENT_LIVE) },
                        onSelectPlayerHistory = { /* history dialog is opened */ },
                        onLoginClick = { viewModel.navigateTo(AppDestination.LOGIN) },
                        onLogoutClick = { viewModel.logout() }
                    )
                    AppDestination.COURT_RENT -> CourtRentScreen(
                        userRole = userRole,
                        players = players,
                        monthlyCourtRent = monthlyCourtRent,
                        selectedMonth = selectedMonth,
                        onBack = { viewModel.navigateTo(AppDestination.DASHBOARD) },
                        onUpdateMonthlyRent = viewModel::updateMonthlyCourtRent,
                        onUpdateSelectedMonth = viewModel::updateSelectedMonth,
                        onAddPlayer = viewModel::addSimplePlayer,
                        onTogglePlayerPaid = viewModel::togglePlayerPaid,
                        onSetPlayerStatus = viewModel::setPlayerStatus,
                        onRenamePlayer = viewModel::renamePlayer,
                        onDeletePlayer = viewModel::deletePlayer,
                        onMarkAllPaid = viewModel::markAllPlayersPaid
                    )
                    AppDestination.TOURNAMENT_LIVE -> TournamentLiveScreen(
                        userRole = userRole,
                        matches = matches,
                        activeMatch = activeMatch,
                        onBack = { viewModel.navigateTo(AppDestination.DASHBOARD) },
                        onSelectMatch = viewModel::selectMatchForScoring,
                        onScorePoint = viewModel::scorePointForTeam,
                        onUndoPoint = viewModel::undoLastPoint
                    )
                    else -> DashboardOverviewScreen(
                        userRole = userRole,
                        monthlyCourtRent = monthlyCourtRent,
                        selectedMonth = selectedMonth,
                        players = players,
                        matches = matches,
                        onNavigateToCourtRent = { viewModel.navigateTo(AppDestination.COURT_RENT) },
                        onNavigateToTournamentLive = { viewModel.navigateTo(AppDestination.TOURNAMENT_LIVE) },
                        onSelectPlayerHistory = { },
                        onLoginClick = { viewModel.navigateTo(AppDestination.LOGIN) },
                        onLogoutClick = { viewModel.logout() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopAppHeader(
    userRole: UserRole,
    paidCount: Int,
    unpaidCount: Int,
    inactiveCount: Int,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    contentDescription = "ShuttlePro",
                    tint = ShuttleGoldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SHUTTLEPRO ARENA",
                    style = MaterialTheme.typography.titleSmall,
                    color = ShuttleGoldAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (userRole == UserRole.ADMIN) "Admin: Full Modify" else "General: View Only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = CourtGreenPrimary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.clickable { onLogout() }.testTag("top_role_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$paidCount Paid • $unpaidCount Due • $inactiveCount Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShuttleGoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
