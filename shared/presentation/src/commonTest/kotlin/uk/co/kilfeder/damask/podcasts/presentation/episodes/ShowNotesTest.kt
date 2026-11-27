package uk.co.kilfeder.damask.podcasts.presentation.episodes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShowNotesTest {
    private fun text(blocks: List<NotesBlock>) = blocks.map { block -> block.spans.joinToString("") { it.text } }

    @Test
    fun plainTextKeepsParagraphsAndLineBreaksAndLinksBareUrls() {
        val blocks = ShowNotes.parse("First line\nsecond line\n\nMore at https://example.com/ep. Thanks &amp; bye")
        assertEquals(listOf("First line\nsecond line", "More at https://example.com/ep. Thanks & bye"), text(blocks))
        assertEquals("https://example.com/ep", blocks[1].spans.single { it.link != null }.link)
    }

    @Test
    fun htmlBecomesStyledBlocks() {
        val html = """<p>Hi <b>bold</b> and <em>italic</em> with <a href="https://x.com/a?b=1&amp;c=2">a link</a>.</p>
            <ul><li>One</li><li>Two</li></ul><h2>Credits</h2><p>Line<br/>break</p>"""
        val blocks = ShowNotes.parse(html)
        assertEquals(listOf("Hi bold and italic with a link.", "One", "Two", "Credits", "Line\nbreak"), text(blocks))
        val first = blocks[0].spans
        assertTrue(first.single { it.text == "bold" }.bold)
        assertTrue(first.single { it.text == "italic" }.italic)
        assertEquals("https://x.com/a?b=1&c=2", first.single { it.text == "a link" }.link)
        assertEquals(listOf(false, true, true, false, false), blocks.map { it.bullet })
        assertTrue(blocks[3].spans.all { it.bold })
    }

    @Test
    fun dropsImagesScriptsAndUnsafeLinks() {
        val html = """<p><img src="t.gif">Text<script>alert(1)</script></p><p>&nbsp;</p>
            <p><a href="javascript:alert(1)">bad</a> <a href='intent://x'>worse</a></p><iframe src="y">z</iframe>"""
        val blocks = ShowNotes.parse(html)
        assertEquals(listOf("Text", "bad worse"), text(blocks))
        assertTrue(blocks.flatMap { it.spans }.all { it.link == null })
    }

    @Test
    fun decodesNumericEntitiesAndHandlesBlankNotes() {
        assertEquals(listOf("It’s 🎙"), text(ShowNotes.parse("<p>It&#8217;s &#x1F399;</p>")))
        assertTrue(ShowNotes.parse(null).isEmpty())
        assertTrue(ShowNotes.parse("  <p> </p> ").isEmpty())
    }

    @Test
    fun spacesAtTheEdgesOfALinkStayOutsideIt() {
        val html = """<p>Instagram:<a href="https://instagram.com/x"> instagram.com/x </a>now</p>"""
        val spans = ShowNotes.parse(html).single().spans
        assertEquals("instagram.com/x", spans.single { it.link != null }.text)
        assertEquals("Instagram: instagram.com/x now", spans.joinToString("") { it.text })
    }
}
