package com.smartvoice.assistant.service.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.smartvoice.assistant.data.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Service wrapping Android's TextToSpeech engine.
 *
 * Provides voice feedback in the same language the user speaks.
 * Automatically selects the correct TTS locale based on the detected language.
 */
class TextToSpeechService(private val context: Context) {

    sealed class TtsState {
        data object Idle : TtsState()
        data object Speaking : TtsState()
        data object Initializing : TtsState()
        data class Error(val message: String) : TtsState()
    }

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage: Language = Language.ENGLISH

    /**
     * Initialize the TTS engine. Call once during the lifecycle of the host component.
     */
    fun initialize(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setLanguage(Language.ENGLISH)
                setupListener()
                _state.value = TtsState.Idle
                onReady?.invoke()
            } else {
                _state.value = TtsState.Error("TTS initialization failed")
            }
        }
    }

    /**
     * Speak the given text in the specified language.
     * The language defaults to the last detected input language.
     */
    fun speak(text: String, language: Language? = null) {
        if (!isInitialized) return

        language?.let { setLanguage(it) }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
        _state.value = TtsState.Speaking
    }

    /**
     * Queue text to speak after the current utterance finishes.
     */
    fun speakQueued(text: String, language: Language? = null) {
        if (!isInitialized) return

        language?.let { setLanguage(it) }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance_${System.currentTimeMillis()}")
    }

    /**
     * Stop speaking immediately.
     */
    fun stop() {
        tts?.stop()
        _state.value = TtsState.Idle
    }

    /**
     * Set the TTS language.
     */
    fun setLanguage(language: Language) {
        currentLanguage = language
        val locale = when (language) {
            Language.ENGLISH -> Locale.US
            Language.AMHARIC -> Locale("am", "ET")
            Language.AFAAN_OROMO -> Locale("om", "ET")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to English if the requested language is not available
            if (language != Language.ENGLISH) {
                tts?.setLanguage(Locale.US)
            }
        }
    }

    /**
     * Check if a specific language is supported by the TTS engine on this device.
     */
    fun isLanguageSupported(language: Language): Boolean {
        if (!isInitialized) return false
        val locale = when (language) {
            Language.ENGLISH -> Locale.US
            Language.AMHARIC -> Locale("am", "ET")
            Language.AFAAN_OROMO -> Locale("om", "ET")
        }
        val result = tts?.isLanguageAvailable(locale)
        return result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /**
     * Set the speech rate (1.0 = normal, 0.5 = half speed, 2.0 = double speed).
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.1f, 3.0f))
    }

    /**
     * Set the pitch (1.0 = normal).
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.1f, 3.0f))
    }

    /**
     * Release all TTS resources.
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error("Speech error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = TtsState.Error("Speech error (code: $errorCode)")
            }
        })
    }
}
