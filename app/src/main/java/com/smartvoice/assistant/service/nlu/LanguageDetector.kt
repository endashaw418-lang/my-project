package com.smartvoice.assistant.service.nlu

import com.smartvoice.assistant.data.model.Language

/**
 * Detects the language of input text using character-set analysis
 * and keyword matching. Works entirely offline.
 */
class LanguageDetector {

    /**
     * Detect language from the given text.
     * Uses Ethiopic Unicode range detection and Oromo keyword matching.
     */
    fun detect(text: String): Language {
        if (text.isBlank()) return Language.ENGLISH

        // Check for Ethiopic (Ge'ez) script characters (U+1200 to U+137F)
        val ethiopicCount = text.count { it.code in 0x1200..0x137F }
        val latinCount = text.count { it.isLetter() && it.code < 0x0250 }

        if (ethiopicCount > latinCount && ethiopicCount > 0) {
            return Language.AMHARIC
        }

        // Check for Afaan Oromo using common keywords and letter patterns
        val lowerText = text.lowercase()
        val oromoScore = calculateOromoScore(lowerText)
        val englishScore = calculateEnglishScore(lowerText)

        return when {
            oromoScore > englishScore && oromoScore > 2 -> Language.AFAAN_OROMO
            else -> Language.ENGLISH
        }
    }

    private fun calculateOromoScore(text: String): Int {
        val oromoKeywords = listOf(
            // Common verbs
            "bani", "cufni", "bilbili", "ergaa", "baasi", "deebisi",
            // Command words
            "banaa", "cufi", "saagi", "haaraa", "mul'isi", "dhaamsa",
            // Common words
            "maaloo", "galatoomaa", "nagaa", "akkam", "eeyyee", "lakki",
            "irra", "keessa", "gara", "wajjin",
            // App-related
            "appii", "bilbila", "muuziqaa", "sa'aatii", "waayifaayii",
            // Doubled vowels common in Oromo
            "aa", "ee", "ii", "oo", "uu"
        )

        var score = 0
        for (keyword in oromoKeywords) {
            if (text.contains(keyword)) score += 2
        }
        // Oromo uses doubled vowels frequently
        val doubledVowelPattern = Regex("(aa|ee|ii|oo|uu)")
        score += doubledVowelPattern.findAll(text).count()

        return score
    }

    private fun calculateEnglishScore(text: String): Int {
        val englishKeywords = listOf(
            "open", "close", "call", "send", "message", "turn", "play",
            "set", "the", "and", "please", "can", "will", "search",
            "find", "show", "take", "what", "where", "how", "alarm",
            "wifi", "bluetooth", "music", "photo", "volume", "brightness"
        )

        var score = 0
        for (keyword in englishKeywords) {
            if (text.contains(keyword)) score += 2
        }
        return score
    }
}
