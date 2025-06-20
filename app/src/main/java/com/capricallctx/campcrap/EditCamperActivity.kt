package com.capricallctx.campcrap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCamperActivity : ComponentActivity() {
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
                    onSaved = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCamperScreen(
    camperId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
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
    
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
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
                    .padding(16.dp),
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
                        onValueChange = { entryDate = it },
                        label = { Text("Entry Date") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = exitDate,
                        onValueChange = { exitDate = it },
                        label = { Text("Exit Date") },
                        modifier = Modifier.weight(1f)
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skipping this year",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = skipping,
                        onCheckedChange = { skipping = it }
                    )
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
                                    skipping = skipping
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
}