package com.smartvoice.assistant.ui.gesture

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartvoice.assistant.data.model.GestureAction
import com.smartvoice.assistant.databinding.FragmentGestureCameraBinding
import com.smartvoice.assistant.service.gesture.GestureRecognitionService
import com.smartvoice.assistant.ui.main.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment that displays the camera preview for hand gesture recognition.
 *
 * Uses CameraX for camera input and MediaPipe for gesture detection.
 * Detected gestures are forwarded to the MainViewModel for execution.
 */
class GestureCameraFragment : Fragment() {

    private var _binding: FragmentGestureCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var gestureService: GestureRecognitionService
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestureCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gestureService = GestureRecognitionService(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (gestureService.initialize()) {
            startCamera()
        } else {
            binding.tvGestureStatus.text = "Gesture recognition unavailable.\nPlease download the MediaPipe model."
        }

        observeGestures()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                binding.tvGestureStatus.text = "Camera initialization failed"
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        gestureService.processFrame(bitmap, System.currentTimeMillis())
        imageProxy.close()
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        return bitmap
    }

    private fun observeGestures() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                gestureService.currentGesture.collect { gesture ->
                    if (gesture != GestureAction.NONE) {
                        binding.tvGestureStatus.text = gesture.description
                        viewModel.handleGesture(gesture)
                    } else {
                        binding.tvGestureStatus.text = "Show a hand gesture..."
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        gestureService.destroy()
        _binding = null
    }
}
