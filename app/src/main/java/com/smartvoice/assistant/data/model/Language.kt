package com.smartvoice.assistant.data.model

/**
 * Supported languages for voice recognition and TTS.
 *
 * @property code BCP-47 language tag used by Android speech APIs
 * @property displayName Human-readable name shown in the UI
 * @property nativeName Name in the language itself
 */
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    ENGLISH("en-US", "English", "English"),
    AMHARIC("am-ET", "Amharic", "አማርኛ"),
    AFAAN_OROMO("om-ET", "Afaan Oromo", "Afaan Oromoo");

    companion object {
        fun fromCode(code: String): Language {
            return entries.firstOrNull { code.startsWith(it.code.substringBefore("-")) }
                ?: ENGLISH
        }

        fun fromDisplayName(name: String): Language {
            return entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true) ||
                    it.nativeName.equals(name, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}
