package com.smartvoice.assistant.ui.main

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartvoice.assistant.SmartVoiceApp
import com.smartvoice.assistant.data.local.CommandHistoryEntity
import com.smartvoice.assistant.data.model.Command
import com.smartvoice.assistant.data.model.CommandResult
import com.smartvoice.assistant.data.model.CommandType
import com.smartvoice.assistant.data.model.GestureAction
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.data.repository.CommandRepository
import com.smartvoice.assistant.service.command.CommandExecutor
import com.smartvoice.assistant.service.nlu.NLUEngine
import com.smartvoice.assistant.service.speech.SpeechRecognitionService
import com.smartvoice.assistant.service.speech.TextToSpeechService
import com.smartvoice.assistant.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the Smart Voice Assistant following MVVM architecture.
 *
 * Orchestrates the full voice command pipeline:
 * Speech Recognition → NLU Processing → Command Execution → TTS Response
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ─── Services ─────────────────────────────────────────────────

    val speechService = SpeechRecognitionService(application)
    val ttsService = TextToSpeechService(application)
    private val nluEngine = NLUEngine()
    private val commandExecutor = CommandExecutor(application)
    private val commandRepository = CommandRepository(
        (application as SmartVoiceApp).database
    )
    private val prefs: SharedPreferences =
        application.getSharedPreferences("smart_voice_prefs", 0)

    // ─── UI State ─────────────────────────────────────────────────

    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _isAutoDetect = MutableStateFlow(true)
    val isAutoDetect: StateFlow<Boolean> = _isAutoDetect.asStateFlow()

    private val _lastResult = MutableStateFlow<CommandResult?>(null)
    val lastResult: StateFlow<CommandResult?> = _lastResult.asStateFlow()

    private val _statusText = MutableStateFlow("Tap the microphone to start")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _gestureEnabled = MutableStateFlow(false)
    val gestureEnabled: StateFlow<Boolean> = _gestureEnabled.asStateFlow()

    private val _continuousListening = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_CONTINUOUS_LISTENING, false)
    )
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    private val _offlineMode = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_OFFLINE_MODE, false)
    )
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = commandRepository
        .getRecentHistory(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Initialization ───────────────────────────────────────────

    init {
        speechService.initialize()
        ttsService.initialize {
            applySpeechRateFromPrefs()
        }
        observeSpeechState()
    }

    // ─── Voice Control ────────────────────────────────────────────

    fun startListening() {
        if (_isAutoDetect.value) {
            speechService.startListeningAutoDetect()
        } else {
            speechService.startListening(_currentLanguage.value)
        }
    }

    fun stopListening() {
        speechService.stopListening()
    }

    /**
     * Process recognized text through the NLU → Execution → TTS pipeline.
     */
    fun processVoiceInput(text: String, confidence: Float, detectedLanguage: Language) {
        viewModelScope.launch {
            _isProcessing.value = true

            // Parse the command using NLU
            val language = if (_isAutoDetect.value) detectedLanguage else _currentLanguage.value
            val command = nluEngine.process(text, language, confidence)

            // Execute the command
            val result = commandExecutor.execute(command)

            // Save to history
            commandRepository.saveCommandResult(command, result)

            // Update UI state
            _lastResult.value = result
            _statusText.value = result.message

            // Provide voice feedback in the detected language
            ttsService.speak(result.message, result.responseLanguage)

            _isProcessing.value = false

            // If continuous listening is enabled, restart listening after TTS finishes
            if (_continuousListening.value) {
                awaitTtsCompletion()
                startListening()
            }
        }
    }

    /**
     * Wait for the TTS engine to finish speaking before re-listening.
     */
    private suspend fun awaitTtsCompletion() {
        while (ttsService.state.value is TextToSpeechService.TtsState.Speaking) {
            delay(200)
        }
    }

    // ─── Gesture Handling ─────────────────────────────────────────

    fun handleGesture(gesture: GestureAction) {
        when (gesture) {
            GestureAction.OPEN_PALM -> startListening()
            GestureAction.CLOSED_FIST -> {
                stopListening()
                ttsService.stop()
            }
            GestureAction.THUMBS_UP -> {
                _statusText.value = "Action confirmed"
                ttsService.speak("Confirmed", _currentLanguage.value)
            }
            GestureAction.THUMBS_DOWN -> {
                _statusText.value = "Action cancelled"
                ttsService.speak("Cancelled", _currentLanguage.value)
            }
            GestureAction.POINTING_UP -> {
                // Scroll up — needs accessibility service
            }
            GestureAction.VICTORY -> {
                val command = Command(
                    type = CommandType.TAKE_SCREENSHOT,
                    rawText = "take screenshot",
                    language = _currentLanguage.value
                )
                viewModelScope.launch {
                    val result = commandExecutor.execute(command)
                    _statusText.value = result.message
                }
            }
            else -> { /* No action */ }
        }
    }

    // ─── Settings ─────────────────────────────────────────────────

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun setAutoDetect(enabled: Boolean) {
        _isAutoDetect.value = enabled
    }

    fun setGestureEnabled(enabled: Boolean) {
        _gestureEnabled.value = enabled
    }

    fun setContinuousListening(enabled: Boolean) {
        _continuousListening.value = enabled
        prefs.edit().putBoolean(Constants.PREF_CONTINUOUS_LISTENING, enabled).apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean(Constants.PREF_OFFLINE_MODE, enabled).apply()
        speechService.setOfflineMode(enabled)
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(Constants.PREF_SPEECH_RATE, rate).apply()
        ttsService.setSpeechRate(rate)
    }

    private fun applySpeechRateFromPrefs() {
        val rate = prefs.getFloat(Constants.PREF_SPEECH_RATE, Constants.DEFAULT_SPEECH_RATE)
        ttsService.setSpeechRate(rate)
    }

    fun clearHistory() {
        viewModelScope.launch {
            commandRepository.clearHistory()
        }
    }

    // ─── Observers ────────────────────────────────────────────────

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechService.state.collect { state ->
                when (state) {
                    is SpeechRecognitionService.RecognitionState.Idle -> {
                        _statusText.value = "Tap the microphone to start"
                    }
                    is SpeechRecognitionService.RecognitionState.Listening -> {
                        _statusText.value = "Listening..."
                    }
                    is SpeechRecognitionService.RecognitionState.Processing -> {
                        _statusText.value = "Processing..."
                    }
                    is SpeechRecognitionService.RecognitionState.Result -> {
                        processVoiceInput(state.text, state.confidence, state.language)
                    }
                    is SpeechRecognitionService.RecognitionState.Error -> {
                        _statusText.value = state.message
                        // In continuous mode, retry on recoverable errors
                        if (_continuousListening.value && state.errorCode != -1) {
                            delay(1000)
                            startListening()
                        }
                    }
                }
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        speechService.destroy()
        ttsService.destroy()
    }
}
