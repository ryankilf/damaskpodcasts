package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsafeIntentLaunchTest {
    private val rule = UnsafeIntentLaunch(Config.empty)

    @Test
    fun `reports launching other apps directly`() {
        val code = """
            import androidx.compose.ui.platform.LocalUriHandler
            fun a(context: Context, intent: Intent) {
                context.startActivity(intent)
                activity.startActivityForResult(intent, 1)
            }
            @Composable fun b() {
                val handler = LocalUriHandler.current
                handler.openUri("mailto:x@example.com")
            }
        """.trimIndent()
        assertEquals(4, rule.lint(code).size)
    }

    @Test
    fun `allows building intents and the safe wrapper`() {
        val code = """
            val pending = PendingIntent.getActivity(context, 0, Intent(context, Main::class.java), 0)
            @Composable fun c() {
                val open = rememberSafeIntent()
                open("https://example.com")
            }
        """.trimIndent()
        assertEquals(0, rule.lint(code).size)
    }
}
