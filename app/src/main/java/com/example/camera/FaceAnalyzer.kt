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

    // 状态记录：用于判断眨眼周期和双击逻辑
    private var isEyesClosed = false
    private var lastBlinkTimestamp: Long = 0
    private var isShakeLocked = false // 增加一个简单的锁，防止摇头动作连续触发

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
        // 修正：使用 image.imageInfo.timestamp (CameraX API)
        faceLandmarker?.detectAsync(mpImage, image.imageInfo.timestamp)
        image.close()
    }

    private fun processResult(result: FaceLandmarkerResult) {
        val landmarksList = result.faceLandmarks()
        if (landmarksList.isNullOrEmpty()) return
        val landmarks = landmarksList[0]

        // --- 1. 眨眼逻辑 (EAR) ---
        val leftEar = calculateEAR(landmarks, 362, 385, 387, 263, 373, 380)
        val rightEar = calculateEAR(landmarks, 33, 160, 158, 133, 153, 144)
        val avgEar = (leftEar + rightEar) / 2f

        if (avgEar < 0.2f) {
            isEyesClosed = true
        } else if (isEyesClosed) {
            // 经历了一个完整的“闭合-睁开”周期
            isEyesClosed = false
            handleBlinkTiming()
        }

        // --- 2. 摇头逻辑 (Shake) ---
        // 核心思路：计算鼻尖在面部左右边缘之间的相对比例
        val nose = landmarks[1]
        val rightFaceEdge = landmarks[234] // 右脸边缘
        val leftFaceEdge = landmarks[454]  // 左脸边缘
        
        // 计算比例：0.5 左右为正脸，越接近 1 或 0 说明头扭得越厉害
        val faceWidth = (leftFaceEdge.x() - rightFaceEdge.x())
        if (faceWidth > 0) {
            val ratio = (nose.x() - rightFaceEdge.x()) / faceWidth
            
            if (ratio > 0.75f || ratio < 0.25f) { // 扭头超过一定幅度
                if (!isShakeLocked) {
                    onActionDetected(FaceAction.SHAKE)
                    isShakeLocked = true
                    // 1秒后解锁摇头，防止连续触发
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isShakeLocked = false
                    }, 1000)
                }
            }
        }
    }

    private fun handleBlinkTiming() {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastBlinkTimestamp
        
        // 如果两次有效眨眼间隔在 150ms ~ 300ms 之间，判定为“双眨眼”
        if (timeDiff in 100..569) {
            onActionDetected(FaceAction.DOUBLE_BLINK)
            lastBlinkTimestamp = 0 // 触发后重置
        } else {
            lastBlinkTimestamp = currentTime
            onActionDetected(FaceAction.BLINK) // 仍触发单次眨眼作为反馈
        }
    }

    private fun calculateEAR(landmarks: List<NormalizedLandmark>, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int): Float {
        val v1 = dist(landmarks[p2], landmarks[p6])
        val v2 = dist(landmarks[p3], landmarks[p5])
        val h = dist(landmarks[p1], landmarks[p4])
        return (v1 + v2) / (2f * h)
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        return sqrt((a.x() - b.x()).toDouble().pow(2.0) + (a.y() - b.y()).toDouble().pow(2.0)).toFloat()
    }

    enum class FaceAction {
        BLINK, DOUBLE_BLINK, NOD, SHAKE
    }
}