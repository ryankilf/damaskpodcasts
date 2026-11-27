package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentDescriptionRequiredTest {
    private val rule = ContentDescriptionRequired(Config.empty)

    @Test
    fun `reports positional contentDescription`() {
        assertEquals(1, rule.lint("""fun f() { Image(painter, null) }""").size)
    }

    @Test
    fun `reports missing contentDescription`() {
        assertEquals(1, rule.lint("""fun f() { AsyncImage(model = url) }""").size)
    }

    @Test
    fun `allows explicit null`() {
        assertEquals(0, rule.lint("""fun f() { Icon(imageVector = icon, contentDescription = null) }""").size)
    }

    @Test
    fun `allows explicit description`() {
        assertEquals(0, rule.lint("""fun f() { Image(painter = p, contentDescription = "Artwork") }""").size)
    }

    @Test
    fun `checks configured wrapper functions`() {
        val configured = ContentDescriptionRequired(TestConfig("functions" to listOf("PodcastArtwork")))
        assertEquals(1, configured.lint("""fun f() { PodcastArtwork(url) }""").size)
        assertEquals(0, configured.lint("""fun f() { Image(painter, null) }""").size)
    }
}

class ClickableRoleRequiredTest {
    private val rule = ClickableRoleRequired(Config.empty)

    @Test
    fun `reports clickable without role`() {
        assertEquals(1, rule.lint("""val m = Modifier.clickable { play() }""").size)
    }

    @Test
    fun `reports null role`() {
        assertEquals(1, rule.lint("""val m = Modifier.toggleable(value = on, role = null) { }""").size)
    }

    @Test
    fun `allows explicit role`() {
        assertEquals(0, rule.lint("""val m = Modifier.clickable(role = Role.Button) { play() }""").size)
    }

    @Test
    fun `ignores unrelated calls`() {
        assertEquals(0, rule.lint("""val m = Modifier.padding(8.dp)""").size)
    }
}

class MinimumTouchTargetTest {
    private val rule = MinimumTouchTarget(Config.empty)

    @Test
    fun `reports small size on clickable chain`() {
        val code = """val m = Modifier.size(24.dp).clickable(role = Role.Button) { }"""
        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `reports small height after clickable`() {
        val code = """val m = Modifier.clickable(role = Role.Button) { }.padding(4.dp).height(32.dp)"""
        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `allows 48dp and above`() {
        val code = """val m = Modifier.size(48.dp).clickable(role = Role.Button) { }"""
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `ignores small sizes on non-interactive chains`() {
        assertEquals(0, rule.lint("""val m = Modifier.size(16.dp).padding(2.dp)""").size)
    }

    @Test
    fun `ignores non-literal sizes`() {
        val code = """val m = Modifier.size(Dimens.icon).clickable(role = Role.Button) { }"""
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `reports disabling minimum interactive size`() {
        val code = """
            fun f() {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) { }
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) { }
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) { }
            }
        """.trimIndent()
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `respects configured minimum`() {
        val ios = MinimumTouchTarget(TestConfig("minSizeDp" to 44))
        assertEquals(0, ios.lint("""val m = Modifier.size(44.dp).clickable(role = Role.Button) { }""").size)
    }
}
