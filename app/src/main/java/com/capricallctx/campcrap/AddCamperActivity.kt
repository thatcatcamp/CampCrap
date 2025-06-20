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
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.capricallctx.campcrap.ui.theme.CampCrapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contact(
    val name: String,
    val email: String = "",
    val phone: String = ""
)

class AddCamperActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled in compose
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampCrapTheme {
                AddCamperScreen(
                    onBack = { finish() },
                    onRequestPermission = { 
                        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCamperScreen(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper(context) }
    
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var hasContactsPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
                == PackageManager.PERMISSION_GRANTED
        ) 
    }
    var showManualDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            scope.launch {
                contacts = loadContacts(context)
            }
        }
    }
    
    // Check permission again when returning to this screen
    LaunchedEffect(Unit) {
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    if (showManualDialog) {
        ManualAddDialog(
            onDismiss = { showManualDialog = false },
            onAddCamper = { name, email, phone, entryDate, exitDate ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dbHelper.addPerson(
                            name = name,
                            email = email,
                            entryDate = entryDate,
                            exitDate = exitDate,
                            year = Constants.CURRENT_YEAR
                        )
                    }
                    successMessage = "Added $name to camp roster!"
                    showSuccessMessage = true
                    showManualDialog = false
                }
            }
        )
    }

    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Campers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add manually")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            
            if (showSuccessMessage) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Add current user button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Add Yourself",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick add yourself to the camp roster",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    dbHelper.addPerson(
                                        name = "Me",
                                        notes = "Camp organizer",
                                        entryDate = "August 24, ${Constants.CURRENT_YEAR}",
                                        exitDate = "September 1, ${Constants.CURRENT_YEAR}",
                                        year = Constants.CURRENT_YEAR
                                    )
                                }
                                successMessage = "Added you to camp roster!"
                                showSuccessMessage = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Me")
                    }
                }
            }
            
            if (!hasContactsPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Import from Contacts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Allow access to contacts to quickly add people from your address book",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Contacts Access")
                        }
                    }
                }
            } else {
                Text(
                    text = "From Contacts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (contacts.isEmpty()) {
                    Text(
                        text = "No contacts found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(contacts) { contact ->
                            ContactItem(
                                contact = contact,
                                onAddContact = { selectedContact ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            dbHelper.addPerson(
                                                name = selectedContact.name,
                                                email = selectedContact.email,
                                                entryDate = "August 24, ${Constants.CURRENT_YEAR}",
                                                exitDate = "September 1, ${Constants.CURRENT_YEAR}",
                                                year = Constants.CURRENT_YEAR
                                            )
                                        }
                                        successMessage = "Added ${selectedContact.name} to camp roster!"
                                        showSuccessMessage = true
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
fun ContactItem(
    contact: Contact,
    onAddContact: (Contact) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Medium
                )
                if (contact.email.isNotEmpty()) {
                    Text(
                        text = contact.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
                onClick = { onAddContact(contact) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onAddCamper: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf("August 24, ${Constants.CURRENT_YEAR}") }
    var exitDate by remember { mutableStateOf("September 1, ${Constants.CURRENT_YEAR}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Camper Manually") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = entryDate,
                    onValueChange = { entryDate = it },
                    label = { Text("Entry Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exitDate,
                    onValueChange = { exitDate = it },
                    label = { Text("Exit Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddCamper(name, email, phone, entryDate, exitDate)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

suspend fun loadContacts(context: android.content.Context): List<Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<Contact>()
    
    val cursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.DATA
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC"
    )
    
    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
        val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
        
        while (it.moveToNext()) {
            val name = it.getString(nameIndex) ?: ""
            val email = it.getString(emailIndex) ?: ""
            
            if (name.isNotEmpty()) {
                contacts.add(Contact(name, email))
            }
        }
    }
    
    // Remove duplicates by name
    contacts.distinctBy { it.name }
}