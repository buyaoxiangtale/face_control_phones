package com.example.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceControlForegroundService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        createNotificationChannel()
        startForeground(1, createNotification())
        startCamera()
    }

    private fun createNotificationChannel() {
        val channelId = "face_control_channel"
        val channelName = "Face Control Service"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val channelId = "face_control_channel"
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FaceControl is active")
            .setContentText("Monitoring face gestures...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            val analyzer = FaceAnalyzer(this) { action ->
                handleFaceAction(action)
            }

            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleFaceAction(action: FaceAnalyzer.FaceAction) {
        val service = FaceAccessibilityService.instance ?: return
        when (action) {
            FaceAnalyzer.FaceAction.DOUBLE_BLINK -> {
                // 双眨眼 -> 向上滑动
                service.performSwipeAction(500f, 1500f, 500f, 500f)
            }
            FaceAnalyzer.FaceAction.SHAKE -> {
                // 摇头 -> 向左滑动 (从右向左)
                service.performSwipeAction(900f, 1000f, 100f, 1000f)
            }
            FaceAnalyzer.FaceAction.BLINK -> {
                // 单次眨眼，暂不执行大动作，可作为预留
            }
            FaceAnalyzer.FaceAction.NOD -> {
                service.performClickAction(500f, 1000f)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}