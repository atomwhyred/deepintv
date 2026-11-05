package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigCodecTest {
    private val rules = ConfigRules(
        widgetNames = setOf("WEATHER", "CALENDAR", "SYSTEM", "CRYPTO"),
        coinIds = setOf("bitcoin", "ethereum", "solana"),
    )

    private fun ok(body: String) = (ConfigCodec.parse(body, rules) as ConfigParse.Ok).config
    private fun invalid(body: String) = (ConfigCodec.parse(body, rules) as ConfigParse.Invalid).reason

    @Test
    fun `a valid body is read field by field`() {
        val config = ok(
            """{"theme":"AUTO","lightFrom":420,"lightTo":1200,"dockTransparency":30,"dockSize":110,
               "widgets":["CRYPTO","WEATHER"],"coins":["bitcoin","solana"],
               "refreshWeather":30,"refreshCrypto":15,"refreshCalendar":60,"refreshSystem":10,"refreshWatchNext":5}""",
        )
        assertEquals("AUTO", config.theme)
        assertEquals(420, config.lightFrom)
        assertEquals(30, config.dockTransparency)
        assertEquals(listOf("CRYPTO", "WEATHER"), config.widgets)
        assertEquals(listOf("bitcoin", "solana"), config.coins)
        assertEquals(30, config.refreshWeather)
        assertEquals(5, config.refreshWatchNext)
    }

    @Test
    fun `only what is sent is set`() {
        val config = ok("""{"dockSize":100}""")
        assertEquals(100, config.dockSize)
        assertEquals(null, config.theme)
        assertEquals(null, config.widgets)
    }

    @Test
    fun `numbers out of range are pulled back in`() {
        val config = ok("""{"dockTransparency":500,"dockSize":5,"lightFrom":-20,"lightTo":99999}""")
        assertEquals(100, config.dockTransparency)
        assertEquals(LauncherSettingsRepository.MIN_SIZE, config.dockSize)
        assertEquals(0, config.lightFrom)
        assertEquals(1439, config.lightTo)
    }

    @Test
    fun `unknown keys, widgets and coins are ignored`() {
        val config = ok("""{"dockSize":90,"evil":"x","widgets":["CRYPTO","HACK","CRYPTO"],"coins":["bitcoin","scamcoin"]}""")
        assertEquals(listOf("CRYPTO"), config.widgets)
        assertEquals(listOf("bitcoin"), config.coins)
    }

    @Test
    fun `an empty widget list is allowed but an empty coin list is not`() {
        assertEquals(emptyList<String>(), ok("""{"widgets":[]}""").widgets)
        assertEquals("coins", invalid("""{"coins":["scamcoin"]}"""))
    }

    @Test
    fun `wrong types are rejected as a whole`() {
        assertEquals("dockSize", invalid("""{"dockTransparency":10,"dockSize":"big"}"""))
        assertEquals("dockSize", invalid("""{"dockSize":100.5}"""))
        assertEquals("theme", invalid("""{"theme":3}"""))
        assertEquals("widgets", invalid("""{"widgets":"WEATHER"}"""))
        assertEquals("widgets", invalid("""{"widgets":[1,2]}"""))
    }

    @Test
    fun `values outside the allowed options are rejected`() {
        assertEquals("theme", invalid("""{"theme":"PINK"}"""))
        assertEquals("refreshWeather", invalid("""{"refreshWeather":1}"""))
        assertEquals("refreshCrypto", invalid("""{"refreshCrypto":0}"""))
    }

    @Test
    fun `broken, empty or oversized bodies are rejected`() {
        assertEquals("malformed", invalid("not json"))
        assertEquals("malformed", invalid("""{"dockSize":"""))
        assertEquals("nothing", invalid("{}"))
        assertEquals("nothing", invalid("""{"unknown":1}"""))
        assertEquals("too_large", invalid("""{"widgets":["${"A".repeat(ConfigCodec.MAX_BODY_CHARS)}"]}"""))
    }

    @Test
    fun `the current settings round-trip through json`() {
        val config = RemoteConfig(theme = "DARK", dockSize = 100, widgets = listOf("WEATHER"), coins = listOf("bitcoin"), refreshSystem = 4)
        val again = ok(ConfigCodec.toJson(config).toString())
        assertEquals(config, again)
        assertTrue(ConfigCodec.toJson(RemoteConfig()).length() == 0)
    }
}
