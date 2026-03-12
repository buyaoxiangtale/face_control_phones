package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.pow
import kotlin.math.sqrt

class FaceAnalyzer(
    context: Context,
    private val onActionDetected: (FaceAction) -> Unit
) : ImageAnalysis.Analyzer {

    private var faceLandmarker: FaceLandmarker? = null
    private var isEyesClosed = false
    private var lastBlinkTimestamp: Long = 0
    private var isShakeLocked = false
    private var isMouthOpened = false

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> processResult(result) }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()
        val rotatedBitmap = if (image.imageInfo.rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        faceLandmarker?.detectAsync(mpImage, image.imageInfo.timestamp)
        image.close()
    }

    private fun processResult(result: FaceLandmarkerResult) {
        val landmarksList = result.faceLandmarks()
        if (landmarksList.isNullOrEmpty()) return
        val landmarks = landmarksList[0]

        // 1. 眨眼逻辑
        val leftEar = calculateEAR(landmarks, 362, 385, 387, 263, 373, 380)
        val rightEar = calculateEAR(landmarks, 33, 160, 158, 133, 153, 144)
        val avgEar = (leftEar + rightEar) / 2f
        if (avgEar < 0.2f) {
            isEyesClosed = true
        } else if (isEyesClosed) {
            isEyesClosed = false
            handleBlinkTiming()
        }

        // 2. 摇头逻辑 (区分左右)
        val nose = landmarks[1]
        val rightFaceEdge = landmarks[234]
        val leftFaceEdge = landmarks[454]
        val faceWidth = (leftFaceEdge.x() - rightFaceEdge.x())
        if (faceWidth > 0) {
            val ratio = (nose.x() - rightFaceEdge.x()) / faceWidth
            if (ratio > 0.75f) { // 扭向一侧
                triggerShake(FaceAction.SHAKE_LEFT)
            } else if (ratio < 0.25f) { // 扭向另一侧
                triggerShake(FaceAction.SHAKE_RIGHT)
            }
        }

        // 3. 点头逻辑 (Simplified Pitch detection)
        val noseTip = landmarks[1]
        val forehead = landmarks[10]
        val chin = landmarks[152]
        val faceHeight = dist(forehead, chin)
        if (faceHeight > 0) {
            val nodRatio = (noseTip.y() - forehead.y()) / faceHeight
            if (nodRatio > 0.6f) { // 鼻尖位置下移，判定为点头
                triggerShake(FaceAction.NOD)
            }
        }

        // 4. 张嘴逻辑
        val upperLip = landmarks[13]
        val lowerLip = landmarks[14]
        val leftMouth = landmarks[78]
        val rightMouth = landmarks[308]
        val mouthHeight = dist(upperLip, lowerLip)
        val mouthWidth = dist(leftMouth, rightMouth)
        if (mouthWidth > 0) {
            val mar = mouthHeight / mouthWidth
            if (mar > 0.5f) {
                if (!isMouthOpened) { onActionDetected(FaceAction.MOUTH_OPEN); isMouthOpened = true }
            } else {
                if (isMouthOpened) { onActionDetected(FaceAction.MOUTH_CLOSE); isMouthOpened = false }
            }
        }
    }

    private fun triggerShake(action: FaceAction) {
        if (!isShakeLocked) {
            onActionDetected(action)
            isShakeLocked = true
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ isShakeLocked = false }, 1000)
        }
    }

    private fun handleBlinkTiming() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlinkTimestamp in 100..569) {
            onActionDetected(FaceAction.DOUBLE_BLINK); lastBlinkTimestamp = 0
        } else {
            lastBlinkTimestamp = currentTime; onActionDetected(FaceAction.BLINK)
        }
    }

    private fun calculateEAR(l: List<NormalizedLandmark>, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int): Float {
        val v1 = dist(l[p2], l[p6]); val v2 = dist(l[p3], l[p5]); val h = dist(l[p1], l[p4])
        return (v1 + v2) / (2f * h)
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float =
        sqrt((a.x() - b.x()).toDouble().pow(2.0) + (a.y() - b.y()).toDouble().pow(2.0)).toFloat()

    enum class FaceAction { BLINK, DOUBLE_BLINK, NOD, SHAKE_LEFT, SHAKE_RIGHT, MOUTH_OPEN, MOUTH_CLOSE }
}