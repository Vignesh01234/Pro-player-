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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.ui.theme.CourtGreenLight
import com.example.ui.theme.CourtGreenPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShuttleGoldAccent
import com.example.ui.viewmodel.UserRole

@Composable
fun TournamentLiveScreen(
    userRole: UserRole,
    matches: List<MatchEntity>,
    activeMatch: MatchEntity?,
    onBack: () -> Unit,
    onSelectMatch: (Long) -> Unit,
    onScorePoint: (MatchEntity, Int) -> Unit,
    onUndoPoint: (MatchEntity) -> Unit
) {
    val liveMatches = matches.filter { it.status == "Live" }
    val scheduledMatches = matches.filter { it.status == "Scheduled" }
    val completedMatches = matches.filter { it.status == "Completed" }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("tournament_live_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("live_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ShuttleGoldAccent)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "TOURNAMENT LIVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShuttleGoldAccent
                            )
                            Text(
                                text = if (userRole == UserRole.ADMIN) "BWF 21-pt Scoring • Admin Scoring Enabled" else "Live Court Scoreboard • General Viewer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (userRole == UserRole.ADMIN) CourtGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
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

            // Live Match Scoreboard Card
            val currentMatch = activeMatch ?: liveMatches.firstOrNull() ?: matches.firstOrNull()
            if (currentMatch != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.5.dp, if (currentMatch.status == "Live") Color(0xFFE53935) else CourtGreenPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("live_scoreboard_card")
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
                                Surface(
                                    color = if (currentMatch.status == "Live") Color(0xFFE53935) else CourtGreenPrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${currentMatch.status.uppercase()} • MATCH #${currentMatch.matchNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = currentMatch.courtName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ShuttleGoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Scoreboard Center
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Team 1
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currentMatch.team1Name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = currentMatch.team1Players,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${currentMatch.g1Team1}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentMatch.g1Team1 >= currentMatch.g1Team2) CourtGreenLight else Color.White
                                    )
                                    if (userRole == UserRole.ADMIN) {
                                        Button(
                                            onClick = { onScorePoint(currentMatch, 1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("score_team1_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Point")
                                        }
                                    }
                                }

                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ShuttleGoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Team 2
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currentMatch.team2Name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = currentMatch.team2Players,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${currentMatch.g1Team2}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentMatch.g1Team2 >= currentMatch.g1Team1) CourtGreenLight else Color.White
                                    )
                                    if (userRole == UserRole.ADMIN) {
                                        Button(
                                            onClick = { onScorePoint(currentMatch, 2) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CourtGreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("score_team2_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Point")
                                        }
                                    }
                                }
                            }

                            if (userRole == UserRole.ADMIN) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { onUndoPoint(currentMatch) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("undo_point_btn")
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Undo Last Rally")
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Viewer mode: only Admin can update points and matches.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fixtures List
            item {
                Text(
                    text = "ALL TOURNAMENT FIXTURES (${matches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShuttleGoldAccent
                )
            }

            items(matches, key = { it.id }) { match ->
                Card(
                    onClick = { onSelectMatch(match.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (match.id == currentMatch?.id) CourtGreenPrimary else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("match_item_${match.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Match #${match.matchNumber} • ${match.courtName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShuttleGoldAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (match.status == "Live") Color(0xFFE53935) else CourtGreenPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = match.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (match.status == "Live") Color.White else CourtGreenLight,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${match.team1Name} vs ${match.team2Name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${match.g1Team1}-${match.g1Team2}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ShuttleGoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
