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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class DayInfo(
    val date: String,
    val dayOfWeek: String,
    val camperCount: Int,
    val arrivals: List<Person>,
    val departures: List<Person>
)

class EventCalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampCrapTheme {
                EventCalendarScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper(context) }
    
    var dayInfoList by remember { mutableStateOf<List<DayInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedDays by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            dayInfoList = withContext(Dispatchers.IO) {
                calculateEventCalendar(dbHelper)
            }
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Event Calendar ${Constants.CURRENT_YEAR}")
                        if (dayInfoList.isNotEmpty()) {
                            Text(
                                text = "${dayInfoList.size} days planned",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                dayInfoList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No event dates found",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add entry/exit dates to campers to see calendar",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayInfoList) { dayInfo ->
                            DayCard(
                                dayInfo = dayInfo,
                                isExpanded = expandedDays.contains(dayInfo.date),
                                onToggleExpanded = {
                                    expandedDays = if (expandedDays.contains(dayInfo.date)) {
                                        expandedDays - dayInfo.date
                                    } else {
                                        expandedDays + dayInfo.date
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCard(
    dayInfo: DayInfo,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                dayInfo.camperCount > 10 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                dayInfo.camperCount > 5 -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayInfo.date,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayInfo.dayOfWeek,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${dayInfo.camperCount} campers",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Show arrival/departure summary
            if (dayInfo.arrivals.isNotEmpty() || dayInfo.departures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (dayInfo.arrivals.isNotEmpty()) {
                        Text(
                            text = "↗️ ${dayInfo.arrivals.size} arriving",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (dayInfo.departures.isNotEmpty()) {
                        Text(
                            text = "↙️ ${dayInfo.departures.size} departing",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            // Expanded details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (dayInfo.arrivals.isNotEmpty()) {
                    Text(
                        text = "Arrivals",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    dayInfo.arrivals.forEach { camper ->
                        Text(
                            text = "• ${camper.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (dayInfo.departures.isNotEmpty()) {
                    if (dayInfo.arrivals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Departures",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    dayInfo.departures.forEach { camper ->
                        Text(
                            text = "• ${camper.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

fun calculateEventCalendar(dbHelper: DatabaseHelper): List<DayInfo> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val campers = dbHelper.getPeopleForYear(Constants.CURRENT_YEAR)
    
    if (campers.isEmpty()) return emptyList()
    
    // Find event date range
    var earliestDate: Calendar? = null
    var latestDate: Calendar? = null
    
    for (camper in campers) {
        if (camper.skipping) continue
        
        try {
            if (camper.entryDate.isNotEmpty()) {
                val entryDate = dateFormat.parse(camper.entryDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
                entryDate?.let { entry ->
                    if (earliestDate == null || entry.before(earliestDate)) {
                        earliestDate = entry
                    }
                }
            }
            
            if (camper.exitDate.isNotEmpty()) {
                val exitDate = dateFormat.parse(camper.exitDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
                exitDate?.let { exit ->
                    if (latestDate == null || exit.after(latestDate)) {
                        latestDate = exit
                    }
                }
            }
        } catch (e: Exception) {
            // Skip invalid dates
        }
    }
    
    if (earliestDate == null || latestDate == null) return emptyList()
    
    val dayInfoList = mutableListOf<DayInfo>()
    val current = Calendar.getInstance().apply { time = earliestDate!!.time }
    
    while (current.timeInMillis <= latestDate!!.timeInMillis) {
        val currentDateStr = dateFormat.format(current.time)
        val dayOfWeek = dayFormat.format(current.time)
        
        val arrivals = mutableListOf<Person>()
        val departures = mutableListOf<Person>()
        var campersOnSite = 0
        
        for (camper in campers) {
            if (camper.skipping) continue
            
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
                
                // Check if arriving today
                if (entryDate != null && entryDate.timeInMillis == current.timeInMillis) {
                    arrivals.add(camper)
                }
                
                // Check if departing today
                if (exitDate != null && exitDate.timeInMillis == current.timeInMillis) {
                    departures.add(camper)
                }
                
                // Check if on site today
                val hasArrived = entryDate?.let { entry -> 
                    current.timeInMillis >= entry.timeInMillis 
                } ?: false
                
                val hasLeft = exitDate?.let { exit -> 
                    current.timeInMillis > exit.timeInMillis 
                } ?: false
                
                if (hasArrived && !hasLeft) {
                    campersOnSite++
                }
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        
        dayInfoList.add(
            DayInfo(
                date = currentDateStr,
                dayOfWeek = dayOfWeek,
                camperCount = campersOnSite,
                arrivals = arrivals,
                departures = departures
            )
        )
        
        current.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return dayInfoList
}