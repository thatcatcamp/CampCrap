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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CrapTapActivity : ComponentActivity() {
    
    private var nfcHelper: NFCHelper? = null
    private var onNfcScanned: ((String) -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize NFC
        nfcHelper = NFCHelper(this)
        
        setContent {
            CampCrapTheme {
                CrapTapScreen(
                    onBack = { finish() },
                    onRequestNfcScan = { callback ->
                        onNfcScanned = callback
                        requestNfcScan()
                    }
                )
            }
        }
    }
    
    private fun requestNfcScan() {
        nfcHelper?.let { helper ->
            if (!helper.isNFCSupported()) {
                // Show error message - NFC not supported
                return
            }
            if (!helper.isNFCEnabled()) {
                // Show error message - NFC not enabled
                return
            }
            
            helper.onTagScanned = { uid ->
                onNfcScanned?.invoke(uid)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        nfcHelper?.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcHelper?.disableForegroundDispatch()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcHelper?.handleNewIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrapTapScreen(
    onBack: () -> Unit,
    onRequestNfcScan: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper(context) }
    
    var isScanning by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<Item?>(null) }
    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var lastSightingMessage by remember { mutableStateOf<String?>(null) }
    
    // Load locations when the screen starts
    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                locations = dbHelper.getLocationsForYear(Constants.CURRENT_YEAR)
            }
        }
    }
    
    // Start NFC scanning automatically
    LaunchedEffect(Unit) {
        isScanning = true
        onRequestNfcScan { scannedUid ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    val item = dbHelper.getItemByNfcUid(scannedUid)
                    if (item != null) {
                        // Record sighting
                        dbHelper.recordSighting(item.id)
                        currentItem = item
                        selectedLocation = locations.find { it.id == item.locationId }
                    } else {
                        // Item not found
                        currentItem = null
                        selectedLocation = null
                    }
                }
                isScanning = false
                lastSightingMessage = if (currentItem != null) "Item sighting recorded!" else null
            }
        }
    }
    
    if (showLocationDialog && currentItem != null) {
        CrapTapLocationSelectionDialog(
            locations = locations,
            onDismiss = { showLocationDialog = false },
            onLocationSelected = { location: Location ->
                selectedLocation = location
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dbHelper.updateItem(
                            id = currentItem!!.id,
                            name = currentItem!!.name,
                            description = currentItem!!.description,
                            camperId = currentItem!!.camperId,
                            locationId = location.id,
                            photoPath = currentItem!!.photoPath,
                            removalStatus = currentItem!!.removalStatus,
                            nfcUid = currentItem!!.nfcUid
                        )
                        // Reload the item to get updated location name
                        currentItem = dbHelper.getItemById(currentItem!!.id)
                    }
                }
                showLocationDialog = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏷️ Crap Tap") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            if (isScanning) {
                // Scanning state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hold NFC tag near device to scan...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (currentItem != null) {
                // Item found - show details
                lastSightingMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = currentItem!!.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (currentItem!!.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentItem!!.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Photo
                        if (currentItem!!.photoPath != null && File(currentItem!!.photoPath).exists()) {
                            AsyncImage(
                                model = File(currentItem!!.photoPath),
                                contentDescription = "Item photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Owner info
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Owner",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Owner: ${currentItem!!.camperName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Location info
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location: ${selectedLocation?.name ?: currentItem!!.locationName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Last sighting info
                        if (currentItem!!.lastSighting != null) {
                            val sightingDate = try {
                                val timestamp = currentItem!!.lastSighting!!.toLong()
                                java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                            } catch (e: Exception) {
                                "Unknown"
                            }
                            Text(
                                text = "Last seen: $sightingDate",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Change location button
                        Button(
                            onClick = { showLocationDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Location")
                        }
                    }
                }
            } else {
                // No item found
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No item found with this NFC tag",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Make sure the item has been tagged in the Edit Item screen.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scan again button
            Button(
                onClick = {
                    isScanning = true
                    currentItem = null
                    selectedLocation = null
                    lastSightingMessage = null
                    onRequestNfcScan { scannedUid ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val item = dbHelper.getItemByNfcUid(scannedUid)
                                if (item != null) {
                                    // Record sighting
                                    dbHelper.recordSighting(item.id)
                                    currentItem = item
                                    selectedLocation = locations.find { it.id == item.locationId }
                                } else {
                                    currentItem = null
                                    selectedLocation = null
                                }
                            }
                            isScanning = false
                            lastSightingMessage = if (currentItem != null) "Item sighting recorded!" else null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Another Item")
            }
        }
    }
}

@Composable
fun CrapTapLocationSelectionDialog(
    locations: List<Location>,
    onDismiss: () -> Unit,
    onLocationSelected: (Location) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Location") },
        text = {
            LazyColumn {
                items(count = locations.size) { index ->
                    val location = locations[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        onClick = { onLocationSelected(location) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = location.name,
                                fontWeight = FontWeight.Medium
                            )
                            if (location.description.isNotEmpty()) {
                                Text(
                                    text = location.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}