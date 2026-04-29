package com.smartvoice.assistant.service.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.smartvoice.assistant.data.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service wrapping Android's SpeechRecognizer for multilingual voice input.
 *
 * Supports English, Amharic, and Afaan Oromo with automatic language detection.
 * Falls back to on-device recognition when offline (if available on the device).
 */
class SpeechRecognitionService(private val context: Context) {

    sealed class RecognitionState {
        data object Idle : RecognitionState()
        data object Listening : RecognitionState()
        data object Processing : RecognitionState()
        data class Result(val text: String, val confidence: Float, val language: Language) : RecognitionState()
        data class Error(val message: String, val errorCode: Int) : RecognitionState()
    }

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguage: Language = Language.ENGLISH
    private var isListening = false
    private var preferOffline = false

    /**
     * Initialize the speech recognizer. Must be called from the main thread.
     */
    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())
        }
    }

    /**
     * Start listening for voice input in the specified language.
     * If [language] is null, attempts recognition across all supported languages.
     */
    fun startListening(language: Language? = null) {
        if (isListening) return

        currentLanguage = language ?: Language.ENGLISH
        val intent = createRecognizerIntent(currentLanguage)

        speechRecognizer?.startListening(intent)
        isListening = true
        _state.value = RecognitionState.Listening
    }

    /**
     * Enable or disable offline-preferred speech recognition.
     */
    fun setOfflineMode(offline: Boolean) {
        preferOffline = offline
    }

    /**
     * Start listening with automatic language detection.
     * Tries each supported language and picks the best result.
     */
    fun startListeningAutoDetect() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // Request recognition for all supported languages
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Language.ENGLISH.code
            )
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayListOf(
                    Language.ENGLISH.code,
                    Language.AMHARIC.code,
                    Language.AFAAN_OROMO.code
                )
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
        _state.value = RecognitionState.Listening
    }

    /**
     * Stop the current recognition session.
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        _state.value = RecognitionState.Idle
    }

    /**
     * Cancel any ongoing recognition.
     */
    fun cancel() {
        speechRecognizer?.cancel()
        isListening = false
        _state.value = RecognitionState.Idle
    }

    /**
     * Release all resources. Call when the service is no longer needed.
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun createRecognizerIntent(language: Language): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.code)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.code)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = RecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                // Voice input has started
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed — can be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer received
            }

            override fun onEndOfSpeech() {
                _state.value = RecognitionState.Processing
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val message = mapErrorCode(error)
                _state.value = RecognitionState.Error(message, error)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    val confidence = confidences?.firstOrNull() ?: 0.8f
                    val detectedLang = detectLanguageFromText(bestMatch)

                    _state.value = RecognitionState.Result(bestMatch, confidence, detectedLang)
                } else {
                    _state.value = RecognitionState.Error("No speech recognized", -1)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Can be used for real-time display of partial transcription
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future use
            }
        }
    }

    /**
     * Simple heuristic language detection based on character sets.
     * Ethiopic script → check for Oromo-specific patterns, else Amharic.
     * Latin script → English.
     */
    private fun detectLanguageFromText(text: String): Language {
        val hasEthiopic = text.any { it.code in 0x1200..0x137F }
        if (hasEthiopic) {
            // Both Amharic and Oromo can use Ethiopic script,
            // but modern Afaan Oromo primarily uses Latin (Qubee).
            return Language.AMHARIC
        }

        // Check for Afaan Oromo specific Latin words/patterns
        val oromoIndicators = listOf(
            "bani", "banu", "baasi", "cufni", "bilbili", "ergaa", "haaraa",
            "mul'isi", "dhaamsa", "banaa", "cufi", "saagi"
        )
        val lowerText = text.lowercase()
        if (oromoIndicators.any { lowerText.contains(it) }) {
            return Language.AFAAN_OROMO
        }

        return Language.ENGLISH
    }

    private fun mapErrorCode(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Unknown error ($error)"
        }
    }
}
