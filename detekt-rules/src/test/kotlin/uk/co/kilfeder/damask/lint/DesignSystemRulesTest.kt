package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SCREEN_PKG = "package uk.co.kilfeder.damask.podcasts.ui.home\n"
private const val THEME_PKG = "package uk.co.kilfeder.damask.podcasts.ui.theme\n"

class NoHardcodedDimensionsTest {
    private val rule = NoHardcodedDimensions(Config.empty)

    @Test
    fun `reports dp and sp literals in screens`() {
        val code = SCREEN_PKG + """val a = Modifier.padding(16.dp); val b = 14.sp; val c = 1.5f.dp"""
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `reports Dp constructors with literals`() {
        assertEquals(1, rule.lint(SCREEN_PKG + """val a = Dp(12f)""").size)
    }

    @Test
    fun `allows zero`() {
        assertEquals(0, rule.lint(SCREEN_PKG + """val a = 0.dp; val b = 0f.dp""").size)
    }

    @Test
    fun `allows tokens`() {
        assertEquals(0, rule.lint(SCREEN_PKG + """val a = Modifier.padding(Spacing.Inset)""").size)
    }

    @Test
    fun `allows literals in the theme package`() {
        assertEquals(0, rule.lint(THEME_PKG + """object Spacing { val Inset = 16.dp }""").size)
    }
}

class NoHardcodedColorsTest {
    private val rule = NoHardcodedColors(Config.empty)

    @Test
    fun `reports literal colours in screens`() {
        val code = SCREEN_PKG + """
            val a = Color(0xFF006A63)
            val b = Color.Red
            val c = Color.hsv(10f, 1f, 1f)
        """.trimIndent()
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `allows roles, Transparent and Unspecified`() {
        val code = SCREEN_PKG + """
            val a = MaterialTheme.colorScheme.primary
            val b = Color.Transparent
            val c = Color.Unspecified
        """.trimIndent()
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `allows literals in the theme package`() {
        assertEquals(0, rule.lint(THEME_PKG + """val primary = Color(0xFF006A63)""").size)
    }
}

class DefaultFontsOnlyTest {
    private val rule = DefaultFontsOnly(Config.empty)

    @Test
    fun `reports bundled and generic families`() {
        val code = """
            val a = FontFamily(Font(Res.font.inter))
            val b = FontFamily.Serif
        """.trimIndent()
        // FontFamily(...), Font(...), FontFamily.Serif
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `allows FontFamily Default`() {
        assertEquals(0, rule.lint("""val a = TextStyle(fontFamily = FontFamily.Default)""").size)
    }
}

class NoHardcodedUiStringsTest {
    private val rule = NoHardcodedUiStrings(Config.empty)

    @Test
    fun `reports literal Text and a11y labels`() {
        val code = """
            fun f() {
                Text("Click me!")
                Text(text = "Played ${'$'}count times")
                Icon(imageVector = i, contentDescription = "Play")
                Modifier.clickable(onClickLabel = "Open show", role = Role.Button) { }
            }
        """.trimIndent()
        assertEquals(4, rule.lint(code).size)
    }

    @Test
    fun `allows resources, pure interpolation and null`() {
        val code = """
            fun f() {
                Text(stringResource(Res.string.play))
                Text("${'$'}title")
                Icon(imageVector = i, contentDescription = null)
            }
        """.trimIndent()
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `ignores previews`() {
        val code = """
            @Preview
            @Composable
            private fun RowPreview() { Text("Sample episode") }
        """.trimIndent()
        assertEquals(0, rule.lint(code).size)
    }
}
