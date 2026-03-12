package com.example.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        if (isPortrait) {
            handlePortraitMode(action, service)
        } else {
            handleLandscapeMode(action, service)
        }
    }

    private fun handlePortraitMode(action: FaceAnalyzer.FaceAction, service: FaceAccessibilityService) {
        when (action) {
            FaceAnalyzer.FaceAction.DOUBLE_BLINK -> {
                // 原始方案：向上滑动
                service.performSwipeAction(500f, 1500f, 500f, 500f)
            }
            FaceAnalyzer.FaceAction.SHAKE_LEFT -> {
                // 左扭头 -> 向左滑动
                service.performSwipeAction(900f, 1000f, 100f, 1000f)
            }
            FaceAnalyzer.FaceAction.SHAKE_RIGHT -> {
                // 右扭头 -> 向右滑动
                service.performSwipeAction(100f, 1000f, 900f, 1000f)
            }
            FaceAnalyzer.FaceAction.MOUTH_OPEN -> {
                service.startContinuousPress(500f, 1000f)
            }
            FaceAnalyzer.FaceAction.MOUTH_CLOSE -> {
                service.stopContinuousPress(500f, 1000f)
            }
            FaceAnalyzer.FaceAction.NOD -> {
                service.performClickAction(500f, 1000f)
            }
            else -> {}
        }
    }

    private fun handleLandscapeMode(action: FaceAnalyzer.FaceAction, service: FaceAccessibilityService) {
        // 横屏方案 (假设屏幕宽 2400, 高 1080)
        when (action) {
            FaceAnalyzer.FaceAction.DOUBLE_BLINK -> {
                // 双眨眼 -> 从左向右拖动 (快进)
                service.performSwipeAction(500f, 540f, 1900f, 540f)
            }
            FaceAnalyzer.FaceAction.NOD -> {
                // 点头 -> 从右向左拖动 (倒退)
                service.performSwipeAction(1900f, 540f, 500f, 540f)
            }
            FaceAnalyzer.FaceAction.MOUTH_OPEN -> {
                // 张嘴 -> 持续长按屏幕中心
                service.startContinuousPress(1200f, 540f)
            }
            FaceAnalyzer.FaceAction.MOUTH_CLOSE -> {
                service.stopContinuousPress(1200f, 540f)
            }
            else -> {}
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