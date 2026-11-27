package uk.co.kilfeder.damask.server.meili

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguagesTest {
    @Test
    fun normalisesTheSpellingsSeenInTheDump() {
        mapOf(
            "en-GB" to "en-gb",
            "en-gb" to "en-gb",
            "en_GB" to "en-gb",
            "EN" to "en",
            " en-US " to "en-us",
            "en_US" to "en-us",
            "eng" to "en",
            "english" to "en",
            "English" to "en",
            "AA" to "aa",
            "SO" to "so",
            "es-419" to "es-419",
            "pt-BR" to "pt-br",
            "fr-FR-x-custom" to "fr-fr",
            "zh-Hant" to "zh",
            "" to "",
            "  " to "",
            "123" to "",
            "deutsch" to "deutsch",
        ).forEach { (raw, tag) -> assertEquals(tag, Languages.tag(raw), "'$raw'") }
    }

    @Test
    fun prefixIsTheLanguagePart() {
        assertEquals("en", Languages.prefix("en-gb"))
        assertEquals("es", Languages.prefix("es-419"))
        assertEquals("", Languages.prefix(""))
        assertEquals("en", PodcastIndex.languagePrefix("ENG"))
    }
}
