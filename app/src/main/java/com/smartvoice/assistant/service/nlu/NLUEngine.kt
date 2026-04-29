package com.smartvoice.assistant.service.nlu

import com.smartvoice.assistant.data.model.Command
import com.smartvoice.assistant.data.model.Language

/**
 * Natural Language Understanding engine that combines language detection
 * and command parsing into a single pipeline.
 *
 * Flow: raw text → detect language → parse command → structured Command object
 */
class NLUEngine {

    private val languageDetector = LanguageDetector()
    private val commandParser = CommandParser()

    /**
     * Process raw voice input into a structured Command.
     *
     * @param text The recognized speech text
     * @param forcedLanguage If set, skips language detection and uses this language
     * @param confidence Recognition confidence from the speech engine
     * @return A parsed Command object with type, parameters, and language
     */
    fun process(
        text: String,
        forcedLanguage: Language? = null,
        confidence: Float = 1.0f
    ): Command {
        val language = forcedLanguage ?: languageDetector.detect(text)
        return commandParser.parse(text, language, confidence)
    }

    /**
     * Detect just the language without parsing a command.
     */
    fun detectLanguage(text: String): Language {
        return languageDetector.detect(text)
    }
}
