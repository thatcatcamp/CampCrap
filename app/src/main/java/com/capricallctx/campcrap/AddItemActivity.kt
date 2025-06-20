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
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddItemActivity : ComponentActivity() {
    
    private lateinit var photoUri: Uri
    private var photoFile: File? = null
    private var onPhotoTaken: (() -> Unit)? = null
    
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
            // Photo was taken successfully, notify the composable
            onPhotoTaken?.invoke()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampCrapTheme {
                AddItemScreen(
                    onBack = { finish() },
                    onRequestCamera = { callback -> 
                        onPhotoTaken = callback
                        requestCameraPermission() 
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onRequestCamera: (((() -> Unit)) -> Unit),
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
    
    var campers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoRefresh by remember { mutableStateOf(0) }
    
    // Update photo file when photoRefresh changes
    LaunchedEffect(photoRefresh) {
        currentPhotoFile = getPhotoFile()
    }
    
    val takePhoto = {
        onRequestCamera {
            photoRefresh++ // Trigger recomposition when photo is taken
        }
    }
    
    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                campers = dbHelper.getAllCampersAndInfrastructureForYear(Constants.CURRENT_YEAR)
                locations = dbHelper.getLocationsForYear(Constants.CURRENT_YEAR)
            }
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
                title = { Text("Add Item") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            if (showSuccessMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Item added successfully!",
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
                            TextButton(onClick = takePhoto) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake Photo")
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
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedCamper != null && selectedLocation != null) {
                        scope.launch {
                            isSaving = true
                            withContext(Dispatchers.IO) {
                                dbHelper.addItem(
                                    name = name,
                                    description = description,
                                    camperId = selectedCamper!!.id,
                                    locationId = selectedLocation!!.id,
                                    photoPath = currentPhotoFile?.absolutePath,
                                    year = Constants.CURRENT_YEAR
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
                Text("Add Item")
            }
        }
    }
}

@Composable
fun CamperSelectionDialog(
    campers: List<Person>,
    onDismiss: () -> Unit,
    onCamperSelected: (Person) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Owner") },
        text = {
            LazyColumn {
                items(campers.size) { index ->
                    val camper = campers[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        onClick = { onCamperSelected(camper) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = camper.name,
                                fontWeight = FontWeight.Medium
                            )
                            if (camper.isInfrastructure) {
                                Text(
                                    text = "Camp Infrastructure",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
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

@Composable
fun LocationSelectionDialog(
    locations: List<Location>,
    onDismiss: () -> Unit,
    onLocationSelected: (Location) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Location") },
        text = {
            LazyColumn {
                items(locations.size) { index ->
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