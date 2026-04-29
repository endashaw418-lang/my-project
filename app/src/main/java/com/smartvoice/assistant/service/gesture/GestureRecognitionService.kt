package com.smartvoice.assistant.service.gesture

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.smartvoice.assistant.data.model.GestureAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand gesture recognition using MediaPipe's GestureRecognizer.
 *
 * Processes camera frames to detect hand gestures and maps them
 * to application actions. Runs on-device for offline support.
 *
 * Supported gestures:
 * - Open palm → activate voice input
 * - Closed fist → stop/cancel
 * - Thumbs up → confirm action
 * - Thumbs down → reject/go back
 * - Pointing up → scroll up
 * - Victory → take screenshot
 */
class GestureRecognitionService(private val context: Context) {

    private val _currentGesture = MutableStateFlow(GestureAction.NONE)
    val currentGesture: StateFlow<GestureAction> = _currentGesture.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var gestureRecognizer: GestureRecognizer? = null

    /**
     * Initialize the MediaPipe gesture recognizer.
     * The model file must be bundled in the app's assets folder.
     *
     * NOTE: You need to download the gesture_recognizer.task model from
     * https://developers.google.com/mediapipe/solutions/vision/gesture_recognizer
     * and place it in app/src/main/assets/
     */
    fun initialize(): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("gesture_recognizer.task")
                .build()

            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumHands(1)
                .setResultListener { result, _ -> processResult(result) }
                .setErrorListener { error -> handleError(error) }
                .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
                .build()

            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
            _isRunning.value = true
            true
        } catch (e: Exception) {
            _isRunning.value = false
            false
        }
    }

    /**
     * Process a camera frame for gesture recognition.
     *
     * @param bitmap The camera frame as a Bitmap
     * @param timestampMs Frame timestamp in milliseconds
     */
    fun processFrame(bitmap: Bitmap, timestampMs: Long) {
        if (gestureRecognizer == null) return

        val mpImage = BitmapImageBuilder(bitmap).build()
        gestureRecognizer?.recognizeAsync(mpImage, timestampMs)
    }

    /**
     * Process the gesture recognition result.
     */
    private fun processResult(result: GestureRecognizerResult) {
        if (result.gestures().isEmpty()) {
            _currentGesture.value = GestureAction.NONE
            return
        }

        val topGesture = result.gestures()[0][0]
        val gestureName = topGesture.categoryName()
        val confidence = topGesture.score()

        // Only accept gestures with confidence > 0.6
        if (confidence < 0.6f) {
            _currentGesture.value = GestureAction.NONE
            return
        }

        _currentGesture.value = mapGestureToAction(gestureName)
    }

    /**
     * Map MediaPipe gesture category names to our GestureAction enum.
     */
    private fun mapGestureToAction(gestureName: String): GestureAction {
        return when (gestureName.lowercase()) {
            "open_palm" -> GestureAction.OPEN_PALM
            "closed_fist" -> GestureAction.CLOSED_FIST
            "thumb_up" -> GestureAction.THUMBS_UP
            "thumb_down" -> GestureAction.THUMBS_DOWN
            "pointing_up" -> GestureAction.POINTING_UP
            "victory" -> GestureAction.VICTORY
            "iloveyou" -> GestureAction.OPEN_PALM // Treat as open palm
            else -> GestureAction.NONE
        }
    }

    private fun handleError(error: RuntimeException) {
        _currentGesture.value = GestureAction.NONE
    }

    /**
     * Stop gesture recognition and release resources.
     */
    fun destroy() {
        gestureRecognizer?.close()
        gestureRecognizer = null
        _isRunning.value = false
    }
}
