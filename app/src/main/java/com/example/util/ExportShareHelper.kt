package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.data.local.FinancialSummary
import com.example.data.local.MatchEntity
import com.example.data.local.TeamStanding
import com.example.data.local.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportShareHelper {

    fun formatNumberIndian(amount: Double): String {
        return NumberFormat.getIntegerInstance(Locale("en", "IN")).format(amount.toLong())
    }

    /**
     * Renders the Indian Rupee symbol (₹, U+20B9) using Android's system SansSerif font family
     * so it never renders as a missing glyph box, followed by formatted numerals.
     */
    fun formatRupeeAnnotated(amount: Double, prefix: String = "", suffix: String = ""): AnnotatedString {
        val formattedDigits = formatNumberIndian(amount)
        return buildAnnotatedString {
            if (prefix.isNotEmpty()) append(prefix)
            withStyle(SpanStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)) {
                append("\u20B9")
            }
            append(formattedDigits)
            if (suffix.isNotEmpty()) append(suffix)
        }
    }

    fun withRupeeSpan(text: String): AnnotatedString {
        return buildAnnotatedString {
            val parts = text.split("₹")
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < parts.lastIndex) {
                    withStyle(SpanStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)) {
                        append("\u20B9")
                    }
                }
            }
        }
    }

    fun buildScheduleSummaryText(matches: List<MatchEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("🏸 SHUTTLEPRO • DAILY MATCH SCHEDULE")
        sb.appendLine("======================================")
        if (matches.isEmpty()) {
            sb.appendLine("No matches currently scheduled.")
        } else {
            matches.forEach { m ->
                val scoreLine = when (m.status) {
                    "Completed" -> "Final: ${m.g1Team1}-${m.g1Team2}, ${m.g2Team1}-${m.g2Team2}" +
                        if (m.g3Team1 > 0 || m.g3Team2 > 0) ", ${m.g3Team1}-${m.g3Team2}" else ""
                    "Live" -> "LIVE (Game ${m.currentGame}): ${m.g1Team1}-${m.g1Team2} | ${m.g2Team1}-${m.g2Team2} | ${m.g3Team1}-${m.g3Team2}"
                    else -> "Est. ${m.estimatedDurationMin}m"
                }
                sb.appendLine("#${m.matchNumber} [${m.stage}] • ${m.scheduledTime} @ ${m.courtName}")
                sb.appendLine("   ${m.team1Name} vs ${m.team2Name}")
                sb.appendLine("   Status: ${m.status} ($scoreLine)")
                sb.appendLine("--------------------------------------")
            }
        }
        return sb.toString()
    }

    fun buildStandingsSummaryText(standings: List<TeamStanding>): String {
        val sb = StringBuilder()
        sb.appendLine("🏆 SHUTTLEPRO • GROUP POINTS TABLE")
        sb.appendLine("======================================")
        val grouped = standings.groupBy { it.groupName }
        for ((grp, list) in grouped) {
            sb.appendLine("\n[$grp]")
            sb.appendLine("Pos | Team               | P | W | L | GD  | PD   | PTS")
            list.forEachIndexed { idx, s ->
                val gdStr = if (s.netGameDiff >= 0) "+${s.netGameDiff}" else "${s.netGameDiff}"
                val pdStr = if (s.netPointDiff >= 0) "+${s.netPointDiff}" else "${s.netPointDiff}"
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "#%-2d | %-18s | %d | %d | %d | %-3s | %-4s | %d",
                        idx + 1,
                        s.teamName.take(18),
                        s.played,
                        s.won,
                        s.lost,
                        gdStr,
                        pdStr,
                        s.tablePoints
                    )
                )
            }
        }
        return sb.toString()
    }

    fun buildPendingPaymentsSummaryText(
        transactions: List<TransactionEntity>,
        summary: FinancialSummary
    ): String {
        val sb = StringBuilder()
        sb.appendLine("💳 SHUTTLEPRO • FINANCIAL & PENDING DUES LEDGER")
        sb.appendLine("======================================")
        sb.appendLine("Total Collected : ₹${summary.totalCollected.toInt()}")
        sb.appendLine("Total Pending   : ₹${summary.totalPendingDues.toInt()}")
        sb.appendLine("Total Expenses  : ₹${summary.totalExpenses.toInt()}")
        sb.appendLine("Net Balance     : ₹${summary.netBalance.toInt()}")
        sb.appendLine("--------------------------------------")
        val pendingList = transactions.filter { it.type == "ENTRY_FEE" && it.paymentStatus != "Paid" }
        if (pendingList.isEmpty()) {
            sb.appendLine("All player entry fees are settled in full!")
        } else {
            sb.appendLine("PENDING / PARTIAL DUES (${pendingList.size}):")
            pendingList.forEach { tx ->
                val due = (tx.amountExpected - tx.amountPaid).toInt()
                sb.appendLine("• ${tx.title} (${tx.teamName}) -> Due: ₹$due [Paid ₹${tx.amountPaid.toInt()}/₹${tx.amountExpected.toInt()}]")
            }
        }
        return sb.toString()
    }

    fun buildDexieAndRoomSchemaCode(): String {
        return """
// ============================================================================
// 1. DEXIE.JS (IndexedDB) SCHEMA IMPLEMENTATION WITH MULTI-TABLE INDEXING
// File: src/db/db.js (For React + Vite / Capacitor PWA cross-platform builds)
// ============================================================================
import Dexie from 'dexie';

export const db = new Dexie('ShuttleProTournamentDB');

db.version(1).stores({
  // Primary key ++id and single + compound indexes for fast queries
  players: '++id, name, phone, skillTier, category, registrationStatus, paymentStatus, [category+skillTier], [registrationStatus+paymentStatus]',
  
  teams: '++id, teamName, category, groupName, registrationStatus, player1Id, player2Id, [groupName+seedRank], [category+registrationStatus]',
  
  courts: '++id, &courtName, surfaceType, isAvailable',
  
  matches: '++id, &matchNumber, stage, format, category, groupName, status, courtName, scheduledTime, winnerTeamId, [stage+groupName], [courtName+scheduledTime], [team1Id+team2Id]',
  
  transactions: '++id, title, type, expenseCategory, playerId, paymentStatus, paymentMode, timestamp, [type+paymentStatus], [paymentMode+timestamp]'
});

// One-line JSON Backup Export helper:
export async function exportTournamentBackupJson() {
  const payload = {
    exportedAt: new Date().toISOString(),
    players: await db.players.toArray(),
    teams: await db.teams.toArray(),
    courts: await db.courts.toArray(),
    matches: await db.matches.toArray(),
    transactions: await db.transactions.toArray(),
  };
  return JSON.stringify(payload, null, 2);
}
        """.trimIndent()
    }

    fun shareTextReport(context: Context, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun launchPaymentReminder(
        context: Context,
        playerName: String,
        phone: String,
        amountDue: Double,
        teamName: String,
        preferWhatsApp: Boolean = true
    ) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val message = "Hi $playerName! Friendly reminder from ShuttlePro Badminton Tournament: " +
            "Your registration balance of ₹${amountDue.toInt()} for '$teamName' is pending. " +
            "Please settle via UPI or at the organizer desk before your next match. Thank you! 🏸"

        try {
            if (preferWhatsApp && cleanPhone.isNotEmpty()) {
                val waPhone = cleanPhone.removePrefix("+")
                val url = "https://wa.me/$waPhone?text=${Uri.encode(message)}"
                val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(waIntent)
            } else if (cleanPhone.isNotEmpty()) {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
                    putExtra("sms_body", message)
                }
                context.startActivity(smsIntent)
            } else {
                shareTextReport(context, "Payment Reminder • $playerName", message)
            }
        } catch (e: Exception) {
            shareTextReport(context, "Payment Reminder • $playerName", message)
        }
    }

    /**
     * Generates a formatted PDF Tournament Summary Report in the app's cache/documents folder
     * using Android's native PdfDocument API and opens the share chooser.
     */
    fun generateAndSharePdfSummary(
        context: Context,
        matches: List<MatchEntity>,
        standings: List<TeamStanding>,
        transactions: List<TransactionEntity>,
        summary: FinancialSummary
    ): String {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.parseColor("#007A53")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 76f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#F5A623")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#007A53")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 10f
            isAntiAlias = true
        }

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText("SHUTTLEPRO • OFFICIAL TOURNAMENT REPORT", 28f, 36f, titlePaint)
        canvas.drawText("BWF Scoring • Schedule • Standings • Financial Ledger  |  Generated: $dateStr", 28f, 56f, subPaint)

        var y = 105f
        canvas.drawText("1. FINANCIAL SUMMARY", 28f, y, sectionPaint)
        y += 18f
        canvas.drawText(
            "Collected: ₹${formatNumberIndian(summary.totalCollected)}   |   Pending Dues: ₹${formatNumberIndian(summary.totalPendingDues)}   |   Expenses: ₹${formatNumberIndian(summary.totalExpenses)}   |   Net Balance: ₹${formatNumberIndian(summary.netBalance)}",
            28f,
            y,
            bodyPaint
        )

        y += 28f
        canvas.drawText("2. GROUP STANDINGS TABLE", 28f, y, sectionPaint)
        y += 16f
        standings.take(8).forEachIndexed { index, s ->
            val line = "${index + 1}. [${s.groupName}] ${s.teamName} (${s.playersLabel}) — P:${s.played} W:${s.won} L:${s.lost} GD:${s.netGameDiff} PD:${s.netPointDiff} PTS:${s.tablePoints}"
            canvas.drawText(line, 28f, y, bodyPaint)
            y += 15f
        }

        y += 18f
        canvas.drawText("3. MATCH SCHEDULE & LIVE SCORES", 28f, y, sectionPaint)
        y += 16f
        matches.take(12).forEach { m ->
            val scoreInfo = if (m.status == "Scheduled") "Scheduled (${m.scheduledTime})"
            else "${m.status}: ${m.g1Team1}-${m.g1Team2}, ${m.g2Team1}-${m.g2Team2}, ${m.g3Team1}-${m.g3Team2}"
            canvas.drawText(
                "#${m.matchNumber} [${m.stage}] ${m.team1Name} vs ${m.team2Name} @ ${m.courtName} — $scoreInfo",
                28f,
                y,
                bodyPaint
            )
            y += 15f
        }

        y += 18f
        canvas.drawText("4. PENDING FEE REMINDERS", 28f, y, sectionPaint)
        y += 16f
        val pending = transactions.filter { it.type == "ENTRY_FEE" && it.paymentStatus != "Paid" }
        if (pending.isEmpty()) {
            canvas.drawText("All player registration fees have been collected.", 28f, y, bodyPaint)
        } else {
            pending.take(8).forEach { tx ->
                val due = (tx.amountExpected - tx.amountPaid).coerceAtLeast(0.0)
                canvas.drawText("• ${tx.title} (${tx.teamName}) — Balance Due: ₹${formatNumberIndian(due)} [${tx.paymentStatus}]", 28f, y, bodyPaint)
                y += 15f
            }
        }

        pdfDocument.finishPage(page)

        val outFile = File(context.cacheDir, "ShuttlePro_Tournament_Report.pdf")
        return try {
            FileOutputStream(outFile).use { stream ->
                pdfDocument.writeTo(stream)
            }
            pdfDocument.close()
            Toast.makeText(context, "PDF generated (${outFile.name}) & sharing report...", Toast.LENGTH_SHORT).show()
            val fullText = buildScheduleSummaryText(matches) + "\n" +
                buildStandingsSummaryText(standings) + "\n" +
                buildPendingPaymentsSummaryText(transactions, summary)
            shareTextReport(context, "ShuttlePro Tournament Report (PDF & Summary)", fullText)
            outFile.absolutePath
        } catch (e: Exception) {
            pdfDocument.close()
            ""
        }
    }

    fun downloadTournamentBackupJson(
        context: Context,
        players: List<com.example.data.local.PlayerEntity>,
        teams: List<com.example.data.local.TeamEntity>,
        courts: List<com.example.data.local.CourtEntity>,
        matches: List<com.example.data.local.MatchEntity>,
        transactions: List<com.example.data.local.TransactionEntity>
    ): String {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sb = StringBuilder()
            sb.appendLine("{")
            sb.appendLine("  \"exportedAt\": \"${Date()}\",")
            sb.appendLine("  \"app\": \"ShuttlePro Tournament\",")
            sb.appendLine("  \"playersCount\": ${players.size},")
            sb.appendLine("  \"teamsCount\": ${teams.size},")
            sb.appendLine("  \"matchesCount\": ${matches.size},")
            sb.appendLine("  \"transactionsCount\": ${transactions.size},")
            sb.appendLine("  \"players\": [")
            players.forEachIndexed { i, p ->
                val safeName = p.name.replace("\"", "\\\"")
                val safePhone = p.phone.replace("\"", "\\\"")
                sb.append("    {\"id\": ${p.id}, \"name\": \"$safeName\", \"phone\": \"$safePhone\", \"tier\": \"${p.skillTier}\", \"category\": \"${p.category}\", \"status\": \"${p.registrationStatus}\"}")
                if (i < players.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("  ],")
            sb.appendLine("  \"teams\": [")
            teams.forEachIndexed { i, t ->
                val safeName = t.teamName.replace("\"", "\\\"")
                val safeP1 = t.player1Name.replace("\"", "\\\"")
                val safeP2 = (t.player2Name ?: "").replace("\"", "\\\"")
                sb.append("    {\"id\": ${t.id}, \"teamName\": \"$safeName\", \"p1\": \"$safeP1\", \"p2\": \"$safeP2\", \"group\": \"${t.groupName}\"}")
                if (i < teams.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("  ],")
            sb.appendLine("  \"matches\": [")
            matches.forEachIndexed { i, m ->
                val safeT1 = m.team1Name.replace("\"", "\\\"")
                val safeT2 = m.team2Name.replace("\"", "\\\"")
                sb.append("    {\"id\": ${m.id}, \"num\": ${m.matchNumber}, \"stage\": \"${m.stage}\", \"t1\": \"$safeT1\", \"t2\": \"$safeT2\", \"court\": \"${m.courtName}\", \"time\": \"${m.scheduledTime}\", \"status\": \"${m.status}\", \"score\": \"${m.g1Team1}-${m.g1Team2}, ${m.g2Team1}-${m.g2Team2}\"}")
                if (i < matches.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("  ],")
            sb.appendLine("  \"transactions\": [")
            transactions.forEachIndexed { i, tx ->
                val safeTitle = tx.title.replace("\"", "\\\"")
                sb.append("    {\"id\": ${tx.id}, \"title\": \"$safeTitle\", \"type\": \"${tx.type}\", \"amountPaid\": ${tx.amountPaid}, \"amountExpected\": ${tx.amountExpected}, \"status\": \"${tx.paymentStatus}\"}")
                if (i < transactions.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("  ]")
            sb.appendLine("}")

            val jsonContent = sb.toString()
            val outFile = File(context.cacheDir, "ShuttlePro_Tournament_Backup_$dateStr.json")
            FileOutputStream(outFile).use { it.write(jsonContent.toByteArray()) }

            Toast.makeText(context, "Backup JSON downloaded (${outFile.name})!", Toast.LENGTH_SHORT).show()
            shareTextReport(context, "ShuttlePro Tournament Backup JSON", jsonContent)
            outFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving backup: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }
}
