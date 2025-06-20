/*
 * Copyright 2025 CAT Camp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.capricallctx.campcrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.launch
import android.util.Log
import android.content.pm.PackageManager
import java.security.MessageDigest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class WaterNeeds(
    val currentCampers: Int,
    val gallonsNeeded: Int,
    val daysRemaining: Int,
    val eventBudget: Int,
    val totalCamperDays: Int,
    val isPreEvent: Boolean
)

fun calculateWaterNeeds(campers: List<Person>): WaterNeeds {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Calendar.getInstance()

    var currentCampersCount = 0
    var maxDaysRemaining = 0
    var totalCamperDays = 0
    var earliestEntry: Calendar? = null
    var latestExit: Calendar? = null
    var isPreEvent = true

    // First pass: calculate event bounds and determine if we're pre-event
    for (camper in campers) {
        if (camper.skipping || camper.entryDate.isEmpty()) continue

        try {
            val entryDate = if (camper.entryDate.isNotEmpty()) {
                dateFormat.parse(camper.entryDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
            } else null

            val exitDate = if (camper.exitDate.isNotEmpty()) {
                dateFormat.parse(camper.exitDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
            } else null

            entryDate?.let { entry ->
                if (earliestEntry == null || entry.before(earliestEntry)) {
                    earliestEntry = entry
                }
                // Check if anyone has arrived yet
                if (today.timeInMillis >= entry.timeInMillis) {
                    isPreEvent = false
                }
            }

            exitDate?.let { exit ->
                if (latestExit == null || exit.after(latestExit)) {
                    latestExit = exit
                }
            }
        } catch (e: Exception) {
            // Skip campers with invalid date formats
        }
    }

    // Second pass: calculate current needs and total budget
    for (camper in campers) {
        if (camper.skipping || camper.entryDate.isEmpty()) continue

        try {
            val entryDate = if (camper.entryDate.isNotEmpty()) {
                dateFormat.parse(camper.entryDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
            } else null

            val exitDate = if (camper.exitDate.isNotEmpty()) {
                dateFormat.parse(camper.exitDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
            } else latestExit // Use event end if no individual exit date

            // Calculate total camper-days for budget (entire event duration per camper)
            if (entryDate != null && exitDate != null) {
                val diffInMillis = exitDate.timeInMillis - entryDate.timeInMillis
                val camperDays = max(1, (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1) // +1 to include both entry and exit days
                totalCamperDays += camperDays
            }

            // Calculate current needs (only for campers currently on site)
            val hasArrived = entryDate?.let { entry ->
                today.timeInMillis >= entry.timeInMillis
            } ?: false

            val hasLeft = exitDate?.let { exit ->
                today.timeInMillis > exit.timeInMillis
            } ?: false

            if (hasArrived && !hasLeft) {
                currentCampersCount++

                // Calculate days remaining for this camper
                val daysLeft = if (exitDate != null) {
                    val diffInMillis = exitDate.timeInMillis - today.timeInMillis
                    val daysRemaining = max(0, (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1) // +1 to include today
                    maxDaysRemaining = max(maxDaysRemaining, daysRemaining)
                    daysRemaining
                } else {
                    // No exit date means they stay indefinitely, assume remaining event duration
                    latestExit?.let { latest ->
                        val diffInMillis = latest.timeInMillis - today.timeInMillis
                        max(0, (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1)
                    } ?: 7
                }
            }
        } catch (e: Exception) {
            // Skip campers with invalid date formats
        }
    }

    val gallonsNeeded = currentCampersCount * 3 * maxDaysRemaining
    val eventBudget = totalCamperDays * 3 // 3 gallons per camper-day

    return WaterNeeds(
        currentCampers = currentCampersCount,
        gallonsNeeded = gallonsNeeded,
        daysRemaining = maxDaysRemaining,
        eventBudget = eventBudget,
        totalCamperDays = totalCamperDays,
        isPreEvent = isPreEvent
    )
}

class MainActivity : ComponentActivity() {

    private lateinit var excelService: ExcelService

    public val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { exportToUri(it) }
    }

    public val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        excelService = ExcelService(this)
        logSHA1Fingerprint()

        setContent {
            CampCrapTheme {
                MainScreen()
            }
        }
    }

    private fun exportToUri(uri: Uri) {
        try {
            val tempFile = excelService.exportData(Constants.CURRENT_YEAR)
            if (tempFile != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                tempFile.delete() // Clean up temp file
                Toast.makeText(this, "Data exported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun importFromUri(uri: Uri) {
        try {
            // Create temporary file
            val tempFile = File(cacheDir, "import_temp.xlsx")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val result = excelService.importData(tempFile, Constants.CURRENT_YEAR, skipExisting = true)
            tempFile.delete() // Clean up

            val message = if (result.hasErrors) {
                "Import completed with errors:\n" +
                "Imported: ${result.totalImported} items\n" +
                "Skipped: ${result.totalSkipped} items\n" +
                "Errors: ${result.errors.size}"
            } else {
                "Import successful!\n" +
                "Campers: ${result.campersImported}\n" +
                "Locations: ${result.locationsImported}\n" +
                "Items: ${result.itemsImported}"
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun logSHA1Fingerprint() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val sha1Mac = digest.joinToString(":") { "%02X".format(it) }
                Log.d("MainActivity", "SHA1 Certificate Fingerprint: $sha1Mac")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting SHA1 fingerprint", e)
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var peopleExist by remember { mutableStateOf(false) }
    var waterNeeds by remember { mutableStateOf(WaterNeeds(0, 0, 0, 0, 0, true)) }
    var showImportDialog by remember { mutableStateOf(false) }

    val dbHelper = remember { DatabaseHelper(context) }
    val activity = context as MainActivity

    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                // Ensure infrastructure camper exists
                dbHelper.ensureInfrastructureCamper(Constants.CURRENT_YEAR)
                // Ensure default Camp Storage location exists
                dbHelper.ensureCampStorageLocation(Constants.CURRENT_YEAR)
                // Check if there are actual people (not including infrastructure)
                peopleExist = dbHelper.hasPeopleForYear(Constants.CURRENT_YEAR)

                // Calculate water needs
                val campers = dbHelper.getPeopleForYear(Constants.CURRENT_YEAR)
                waterNeeds = calculateWaterNeeds(campers)
            }
        }
    }

    LandingScreen(
        peopleExist = peopleExist,
        waterNeeds = waterNeeds,
        onCamperAction = {
            val intent = if (peopleExist) {
                Intent(context, CamperListActivity::class.java)
            } else {
                Intent(context, AddCamperActivity::class.java)
            }
            context.startActivity(intent)
        },
        onViewLocations = {
            val intent = Intent(context, LocationListActivity::class.java)
            context.startActivity(intent)
        },
        onViewCrap = {
            val intent = Intent(context, ItemListActivity::class.java)
            context.startActivity(intent)
        },
        onViewCalendar = {
            val intent = Intent(context, EventCalendarActivity::class.java)
            context.startActivity(intent)
        },
        onExportData = {
            val fileName = "CampCrap_${Constants.CURRENT_YEAR}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.xlsx"
            activity.exportLauncher.launch(fileName)
        },
        onImportData = {
            showImportDialog = true
        }
    )

    // Import confirmation dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Data") },
            text = {
                Text("This will import data from an Excel file. Existing data with the same names will be skipped. Continue?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    activity.importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LandingScreen(
    peopleExist: Boolean,
    waterNeeds: WaterNeeds,
    onCamperAction: () -> Unit,
    onViewLocations: () -> Unit,
    onViewCrap: () -> Unit,
    onViewCalendar: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.truck),
            contentDescription = "Truck background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camp Crap",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = Constants.CURRENT_YEAR,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Water needs summary
            if (waterNeeds.eventBudget > 0) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (waterNeeds.isPreEvent) {
                        // Pre-event: Show budget for purchasing
                        Text(
                            text = "💧 Water Budget",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${waterNeeds.eventBudget} gallons",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Cyan,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Total needed for ${waterNeeds.totalCamperDays} camper-days",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "📅 Event starts soon - purchase now!",
                            fontSize = 14.sp,
                            color = Color.Yellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (waterNeeds.currentCampers > 0) {
                        // During event: Show current needs
                        Text(
                            text = "💧 Water Needed",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${waterNeeds.gallonsNeeded} gallons",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Cyan,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${waterNeeds.currentCampers} campers • ${waterNeeds.daysRemaining} days remaining",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        // Show budget comparison
                        Text(
                            text = "Event budget: ${waterNeeds.eventBudget} gallons total",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // Post-arrivals but no one currently on site
                        Text(
                            text = "💧 No Current Water Needs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "0 gallons",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "No campers currently on site",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (peopleExist) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Add entry/exit dates to calculate water needs",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onCamperAction,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (!peopleExist) "Add Your First Camper" else "View Campers")
            }

            Button(
                onClick = onViewLocations,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("View Locations")
            }

            Button(
                onClick = onViewCrap,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("View Crap")
            }

            Button(
                onClick = onViewCalendar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Event Calendar")
            }

            // Export/Import section
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📤 Export Excel")
                }

                Button(
                    onClick = onImportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📥 Import Excel")
                }
            }
        }

        Text(
            text = "Copyright 2025 © CAT Camp",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    CampCrapTheme {
        LandingScreen(
            peopleExist = true,
            waterNeeds = WaterNeeds(
                currentCampers = 0,
                gallonsNeeded = 0,
                daysRemaining = 0,
                eventBudget = 420,
                totalCamperDays = 140,
                isPreEvent = true
            ),
            onCamperAction = {},
            onViewLocations = {},
            onViewCrap = {},
            onViewCalendar = {},
            onExportData = {},
            onImportData = {}
        )
    }
}
