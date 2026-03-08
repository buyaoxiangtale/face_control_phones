package com.example.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camera.ui.theme.CameraTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartService = { startFaceControlService() },
                        onOpenAccessibility = { openAccessibilitySettings() },
                        onOpenOverlay = { openOverlaySettings() },
                        onRequestCamera = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        }
    }

    private fun startFaceControlService() {
        val intent = Intent(this, FaceControlForegroundService::class.java)
        startForegroundService(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}

@Composable
fun MainScreen(
    onStartService: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "FaceControl MVP", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onRequestCamera, modifier = Modifier.fillMaxWidth()) {
            Text("1. Request Camera Permission")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
            Text("2. Enable Accessibility Service")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = onOpenOverlay, modifier = Modifier.fillMaxWidth()) {
            Text("3. Enable Overlay Permission")
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onStartService,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("START FACECONTROL")
        }
    }
}