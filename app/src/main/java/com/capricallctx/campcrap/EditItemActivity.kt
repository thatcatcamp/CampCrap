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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditItemActivity : ComponentActivity() {
    
    private lateinit var photoUri: Uri
    private var photoFile: File? = null
    private var onPhotoTaken: (() -> Unit)? = null
    private var nfcHelper: NFCHelper? = null
    private var onNfcScanned: ((String) -> Unit)? = null
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        }
    }
    
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onPhotoTaken?.invoke()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val itemId = intent.getLongExtra("ITEM_ID", -1)
        if (itemId == -1L) {
            finish()
            return
        }
        
        // Initialize NFC
        nfcHelper = NFCHelper(this)
        
        setContent {
            CampCrapTheme {
                EditItemScreen(
                    itemId = itemId,
                    onBack = { finish() },
                    onRequestCamera = { callback -> 
                        onPhotoTaken = callback
                        requestCameraPermission() 
                    },
                    onRequestNfcScan = { callback ->
                        onNfcScanned = callback
                        requestNfcScan()
                    },
                    getPhotoFile = { photoFile },
                    onItemSaved = { finish() }
                )
            }
        }
    }
    
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        photoFile = File(filesDir, "IMG_${timeStamp}.jpg")
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile!!
        )
        takePictureLauncher.launch(photoUri)
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
fun EditItemScreen(
    itemId: Long,
    onBack: () -> Unit,
    onRequestCamera: (((() -> Unit)) -> Unit),
    onRequestNfcScan: ((String) -> Unit) -> Unit,
    getPhotoFile: () -> File?,
    onItemSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper(context) }
    
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCamper by remember { mutableStateOf<Person?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var showCamperDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    var campers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoRefresh by remember { mutableStateOf(0) }
    var originalPhotoPath by remember { mutableStateOf<String?>(null) }
    var removalStatus by remember { mutableStateOf("active") }
    var nfcUid by remember { mutableStateOf<String?>(null) }
    var lastSighting by remember { mutableStateOf<String?>(null) }
    
    // Update photo file when photoRefresh changes
    LaunchedEffect(photoRefresh) {
        currentPhotoFile = getPhotoFile()
    }
    
    val takePhoto = {
        onRequestCamera {
            photoRefresh++
        }
    }
    
    // Load item data and options
    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val item = dbHelper.getItemById(itemId)
                item?.let {
                    name = it.name
                    description = it.description
                    originalPhotoPath = it.photoPath
                    removalStatus = it.removalStatus
                    nfcUid = it.nfcUid
                    lastSighting = it.lastSighting
                    if (it.photoPath != null) {
                        currentPhotoFile = File(it.photoPath)
                    }
                    
                    // Load campers and locations
                    campers = dbHelper.getAllCampersAndInfrastructureForYear(Constants.CURRENT_YEAR)
                    locations = dbHelper.getLocationsForYear(Constants.CURRENT_YEAR)
                    
                    // Set selected camper and location
                    selectedCamper = campers.find { camper -> camper.id == it.camperId }
                    selectedLocation = locations.find { location -> location.id == it.locationId }
                }
            }
            isLoading = false
        }
    }
    
    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onItemSaved()
        }
    }
    
    if (showCamperDialog) {
        CamperSelectionDialog(
            campers = campers,
            onDismiss = { showCamperDialog = false },
            onCamperSelected = { camper ->
                selectedCamper = camper
                showCamperDialog = false
            }
        )
    }
    
    if (showLocationDialog) {
        LocationSelectionDialog(
            locations = locations,
            onDismiss = { showLocationDialog = false },
            onLocationSelected = { location ->
                selectedLocation = location
                showLocationDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                if (showSuccessMessage) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "Item updated successfully!",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Photo section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Photo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Display photo if available
                        if (currentPhotoFile?.exists() == true) {
                            AsyncImage(
                                model = currentPhotoFile,
                                contentDescription = "Item photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = takePhoto,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retake Photo")
                                }
                                if (originalPhotoPath != null) {
                                    TextButton(
                                        onClick = {
                                            currentPhotoFile = if (originalPhotoPath != null) File(originalPhotoPath!!) else null
                                            photoRefresh++
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Restore Original")
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = takePhoto,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Take Photo")
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                // Camper selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showCamperDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Owner",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCamper?.name ?: "Select owner",
                                fontSize = 16.sp,
                                fontWeight = if (selectedCamper != null) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedCamper != null) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Select",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
                
                // Location selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showLocationDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Location",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedLocation?.name ?: "Select location",
                                fontSize = 16.sp,
                                fontWeight = if (selectedLocation != null) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedLocation != null) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Select",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
                
                // NFC Section
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
                            text = "NFC Tag",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (nfcUid != null) {
                            Text(
                                text = "Current NFC UID: $nfcUid",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        if (lastSighting != null) {
                            val sightingDate = try {
                                val timestamp = lastSighting!!.toLong()
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
                        
                        Button(
                            onClick = { 
                                onRequestNfcScan { scannedUid ->
                                    nfcUid = scannedUid
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (nfcUid != null) "Update NFC Tag" else "Scan NFC Tag")
                        }
                    }
                }
                
                // Removal section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Remove Item",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Current Status: ${removalStatus.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (removalStatus == "active") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mark this item as removed from camp:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { removalStatus = "trashed" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Trashed", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { removalStatus = "taken_home" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Taken Home", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { removalStatus = "donated" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Donated", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This item has been removed from camp.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { removalStatus = "active" },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Mark as Active Again")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        if (name.isNotBlank() && selectedCamper != null && selectedLocation != null) {
                            scope.launch {
                                isSaving = true
                                withContext(Dispatchers.IO) {
                                    dbHelper.updateItem(
                                        id = itemId,
                                        name = name,
                                        description = description,
                                        camperId = selectedCamper!!.id,
                                        locationId = selectedLocation!!.id,
                                        photoPath = currentPhotoFile?.absolutePath,
                                        removalStatus = removalStatus,
                                        nfcUid = nfcUid
                                    )
                                }
                                isSaving = false
                                showSuccessMessage = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && selectedCamper != null && selectedLocation != null && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Update Item")
                }
            }
        }
    }
}