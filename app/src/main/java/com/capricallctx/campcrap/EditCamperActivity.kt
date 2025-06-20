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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.DateRange
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

class EditCamperActivity : ComponentActivity() {

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
            onPhotoTaken?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val camperId = intent.getLongExtra("CAMPER_ID", -1L)
        if (camperId == -1L) {
            finish()
            return
        }

        setContent {
            CampCrapTheme {
                EditCamperScreen(
                    camperId = camperId,
                    onBack = { finish() },
                    onSaved = { finish() },
                    onRequestCamera = { callback ->
                        onPhotoTaken = callback
                        requestCameraPermission()
                    },
                    getPhotoFile = { photoFile }
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
        photoFile = File(filesDir, "CAMPER_${timeStamp}.jpg")
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
fun EditCamperScreen(
    camperId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onRequestCamera: (((() -> Unit)) -> Unit),
    getPhotoFile: () -> File?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper(context) }

    var camper by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf("") }
    var exitDate by remember { mutableStateOf("") }
    var campName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var skipping by remember { mutableStateOf(false) }
    var yearsAttended by remember { mutableStateOf("") }
    var hasTicketCurrentYear by remember { mutableStateOf(false) }
    var paidDuesCurrentYear by remember { mutableStateOf(false) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoRefresh by remember { mutableStateOf(0) }
    var originalPhotoPath by remember { mutableStateOf<String?>(null) }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showEntryDatePicker by remember { mutableStateOf(false) }
    var showExitDatePicker by remember { mutableStateOf(false) }

    // Update photo file when photoRefresh changes
    LaunchedEffect(photoRefresh) {
        currentPhotoFile = getPhotoFile()
    }

    val takePhoto = {
        onRequestCamera {
            photoRefresh++
        }
    }

    LaunchedEffect(camperId) {
        scope.launch {
            val person = withContext(Dispatchers.IO) {
                dbHelper.getPersonById(camperId)
            }
            camper = person
            person?.let {
                name = it.name
                email = it.email
                realName = it.realName
                entryDate = it.entryDate
                exitDate = it.exitDate
                campName = it.campName
                notes = it.notes
                skipping = it.skipping
                yearsAttended = it.yearsAttended
                hasTicketCurrentYear = it.hasTicketCurrentYear
                paidDuesCurrentYear = it.paidDuesCurrentYear
                originalPhotoPath = it.photoPath
                if (it.photoPath != null) {
                    currentPhotoFile = File(it.photoPath)
                }
            }
            isLoading = false
        }
    }

    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Camper") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (camper == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Camper not found")
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
                            text = "Camper updated successfully!",
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

                        if (currentPhotoFile?.exists() == true) {
                            AsyncImage(
                                model = currentPhotoFile,
                                contentDescription = "Camper photo",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = takePhoto) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retake Photo")
                                }
                                if (originalPhotoPath != null) {
                                    TextButton(
                                        onClick = {
                                            currentPhotoFile = if (originalPhotoPath != null) File(originalPhotoPath!!) else null
                                            photoRefresh++
                                        }
                                    ) {
                                        Text("Restore Original")
                                    }
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "No photo",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = takePhoto) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Photo")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = realName,
                    onValueChange = { realName = it },
                    label = { Text("Real Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = entryDate,
                        onValueChange = { },
                        label = { Text("Entry Date") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showEntryDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select entry date")
                            }
                        },
                        placeholder = { Text("YYYY-MM-DD") }
                    )

                    OutlinedTextField(
                        value = exitDate,
                        onValueChange = { },
                        label = { Text("Exit Date") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showExitDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select exit date")
                            }
                        },
                        placeholder = { Text("YYYY-MM-DD") }
                    )
                }

                OutlinedTextField(
                    value = campName,
                    onValueChange = { campName = it },
                    label = { Text("Camp Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                OutlinedTextField(
                    value = yearsAttended,
                    onValueChange = { yearsAttended = it },
                    label = { Text("Years Attended (e.g., 2020,2021,2022,2024)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Comma-separated years") }
                )

                // Current Year Status Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "2025 Status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Has Ticket",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = hasTicketCurrentYear,
                                onCheckedChange = { hasTicketCurrentYear = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Paid Dues",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = paidDuesCurrentYear,
                                onCheckedChange = { paidDuesCurrentYear = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (skipping) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Skipping this year",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = skipping,
                                onCheckedChange = { skipping = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val success = withContext(Dispatchers.IO) {
                                dbHelper.updatePerson(
                                    id = camperId,
                                    name = name,
                                    email = email,
                                    realName = realName,
                                    entryDate = entryDate,
                                    exitDate = exitDate,
                                    campName = campName,
                                    notes = notes,
                                    skipping = skipping,
                                    yearsAttended = yearsAttended,
                                    hasTicketCurrentYear = hasTicketCurrentYear,
                                    paidDuesCurrentYear = paidDuesCurrentYear,
                                    photoPath = currentPhotoFile?.absolutePath
                                )
                            }
                            isSaving = false
                            if (success) {
                                showSuccessMessage = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Changes")
                }
            }
        }
    }

    // Entry Date Picker
    if (showEntryDatePicker) {
        DatePickerDialog(
            initialDate = entryDate,
            onDateSelected = { selectedDate ->
                entryDate = selectedDate
                showEntryDatePicker = false
            },
            onDismiss = { showEntryDatePicker = false }
        )
    }

    // Exit Date Picker
    if (showExitDatePicker) {
        DatePickerDialog(
            initialDate = exitDate,
            onDateSelected = { selectedDate ->
                exitDate = selectedDate
                showExitDatePicker = false
            },
            onDismiss = { showExitDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // Parse initial date if provided
    if (initialDate.isNotEmpty()) {
        try {
            dateFormat.parse(initialDate)?.let { date ->
                calendar.time = date
            }
        } catch (e: Exception) {
            // Use current date if parsing fails
        }
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        timeInMillis = millis
                    }
                    val formattedDate = dateFormat.format(selectedCalendar.time)
                    onDateSelected(formattedDate)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
