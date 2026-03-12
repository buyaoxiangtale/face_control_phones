package com.example.camera

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class FaceAccessibilityService : AccessibilityService() {

    private var currentStroke: GestureDescription.StrokeDescription? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPressing = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun performClickAction(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 开始持续按压
    fun startContinuousPress(x: Float, y: Float) {
        if (isPressing) return
        isPressing = true
        
        val path = Path().apply { moveTo(x, y) }
        // 初始按压
        currentStroke = GestureDescription.StrokeDescription(path, 0, 200, true)
        val gesture = GestureDescription.Builder().addStroke(currentStroke!!).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (isPressing) {
                    continuePress(x, y)
                }
            }
        }, null)
    }

    // 续期按压
    private fun continuePress(x: Float, y: Float) {
        if (!isPressing) return
        
        val path = Path().apply { moveTo(x, y) }
        currentStroke = currentStroke?.continueStroke(path, 0, 200, true)
        currentStroke?.let {
            val gesture = GestureDescription.Builder().addStroke(it).build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    handler.postDelayed({
                        if (isPressing) continuePress(x, y)
                    }, 10)
                }
            }, null)
        }
    }

    // 停止按压
    fun stopContinuousPress(x: Float, y: Float) {
        isPressing = false
        currentStroke?.let {
            val path = Path().apply { moveTo(x, y) }
            val lastStroke = it.continueStroke(path, 0, 100, false)
            val gesture = GestureDescription.Builder().addStroke(lastStroke).build()
            dispatchGesture(gesture, null, null)
            currentStroke = null
        }
    }

    fun performSwipeAction(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    companion object {
        var instance: FaceAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
}