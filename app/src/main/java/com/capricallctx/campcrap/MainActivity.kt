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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        logSHA1Fingerprint()
        
        setContent {
            CampCrapTheme {
                MainScreen()
            }
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
    
    val dbHelper = remember { DatabaseHelper(context) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                // Ensure infrastructure camper exists
                dbHelper.ensureInfrastructureCamper(Constants.CURRENT_YEAR)
                // Ensure default Camp Storage location exists
                dbHelper.ensureCampStorageLocation(Constants.CURRENT_YEAR)
                // Check if there are actual people (not including infrastructure)
                peopleExist = dbHelper.hasPeopleForYear(Constants.CURRENT_YEAR)
            }
        }
    }

    LandingScreen(
        peopleExist = peopleExist,
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
        }
    )
}

@Composable
fun LandingScreen(
    peopleExist: Boolean,
    onCamperAction: () -> Unit,
    onViewLocations: () -> Unit,
    onViewCrap: () -> Unit
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
        LandingScreen(false, {}, {}, {})
    }
}