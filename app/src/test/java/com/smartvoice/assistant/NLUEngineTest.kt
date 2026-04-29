package com.smartvoice.assistant

import com.smartvoice.assistant.data.model.CommandType
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.service.nlu.CommandParser
import com.smartvoice.assistant.service.nlu.LanguageDetector
import com.smartvoice.assistant.service.nlu.NLUEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the NLU engine, language detection, and command parsing.
 */
class NLUEngineTest {

    private val nluEngine = NLUEngine()
    private val languageDetector = LanguageDetector()
    private val commandParser = CommandParser()

    // ─── Language Detection ───────────────────────────────────────

    @Test
    fun `detects English text`() {
        assertEquals(Language.ENGLISH, languageDetector.detect("open WhatsApp"))
        assertEquals(Language.ENGLISH, languageDetector.detect("turn on wifi"))
        assertEquals(Language.ENGLISH, languageDetector.detect("call mom"))
    }

    @Test
    fun `detects Amharic text`() {
        assertEquals(Language.AMHARIC, languageDetector.detect("ዋትስአፕ ክፈት"))
        assertEquals(Language.AMHARIC, languageDetector.detect("ደውል ለእማማ"))
    }

    @Test
    fun `detects Afaan Oromo text`() {
        assertEquals(Language.AFAAN_OROMO, languageDetector.detect("bilbili gara haadha"))
        assertEquals(Language.AFAAN_OROMO, languageDetector.detect("waayifaayii banaa"))
    }

    // ─── English Command Parsing ──────────────────────────────────

    @Test
    fun `parses open app command`() {
        val command = commandParser.parse("open WhatsApp", Language.ENGLISH)
        assertEquals(CommandType.OPEN_APP, command.type)
        assertEquals("whatsapp", command.parameters["app_name"])
    }

    @Test
    fun `parses call command`() {
        val command = commandParser.parse("call John", Language.ENGLISH)
        assertEquals(CommandType.CALL_CONTACT, command.type)
        assertEquals("john", command.parameters["contact_name"])
    }

    @Test
    fun `parses send message command`() {
        val command = commandParser.parse("send message to Maria saying hello", Language.ENGLISH)
        assertEquals(CommandType.SEND_MESSAGE, command.type)
    }

    @Test
    fun `parses wifi toggle command`() {
        val commandOn = commandParser.parse("turn on WiFi", Language.ENGLISH)
        assertEquals(CommandType.TOGGLE_WIFI, commandOn.type)
        assertEquals("on", commandOn.parameters["state"])

        val commandOff = commandParser.parse("turn off WiFi", Language.ENGLISH)
        assertEquals(CommandType.TOGGLE_WIFI, commandOff.type)
        assertEquals("off", commandOff.parameters["state"])
    }

    @Test
    fun `parses play music command`() {
        val command = commandParser.parse("play music", Language.ENGLISH)
        assertEquals(CommandType.PLAY_MUSIC, command.type)
    }

    @Test
    fun `parses set alarm command`() {
        val command = commandParser.parse("set alarm for 7:30 am", Language.ENGLISH)
        assertEquals(CommandType.SET_ALARM, command.type)
    }

    @Test
    fun `parses volume commands`() {
        val up = commandParser.parse("volume up", Language.ENGLISH)
        assertEquals(CommandType.SET_VOLUME, up.type)
        assertEquals("up", up.parameters["direction"])

        val down = commandParser.parse("volume down", Language.ENGLISH)
        assertEquals(CommandType.SET_VOLUME, down.type)
        assertEquals("down", down.parameters["direction"])
    }

    @Test
    fun `parses search command`() {
        val command = commandParser.parse("search for weather today", Language.ENGLISH)
        assertEquals(CommandType.SEARCH_WEB, command.type)
    }

    @Test
    fun `parses battery check command`() {
        val command = commandParser.parse("check battery", Language.ENGLISH)
        assertEquals(CommandType.CHECK_BATTERY, command.type)
    }

    @Test
    fun `handles unknown commands`() {
        val command = commandParser.parse("do something random and weird", Language.ENGLISH)
        assertEquals(CommandType.UNKNOWN, command.type)
    }

    // ─── Amharic Command Parsing ──────────────────────────────────

    @Test
    fun `parses Amharic open app command`() {
        val command = commandParser.parse("ዋትስአፕ ክፈት", Language.AMHARIC)
        assertEquals(CommandType.OPEN_APP, command.type)
    }

    @Test
    fun `parses Amharic call command`() {
        val command = commandParser.parse("ወደ ማማ ደውል", Language.AMHARIC)
        assertEquals(CommandType.CALL_CONTACT, command.type)
    }

    // ─── Afaan Oromo Command Parsing ──────────────────────────────

    @Test
    fun `parses Oromo open app command`() {
        val command = commandParser.parse("WhatsApp bani", Language.AFAAN_OROMO)
        assertEquals(CommandType.OPEN_APP, command.type)
    }

    @Test
    fun `parses Oromo call command`() {
        val command = commandParser.parse("gara haadha bilbili", Language.AFAAN_OROMO)
        assertEquals(CommandType.CALL_CONTACT, command.type)
    }

    // ─── Full Pipeline ────────────────────────────────────────────

    @Test
    fun `full pipeline processes English command`() {
        val command = nluEngine.process("open WhatsApp")
        assertEquals(CommandType.OPEN_APP, command.type)
        assertEquals(Language.ENGLISH, command.language)
    }

    @Test
    fun `full pipeline with forced language`() {
        val command = nluEngine.process("ክፈት", forcedLanguage = Language.AMHARIC)
        assertEquals(Language.AMHARIC, command.language)
    }
}
