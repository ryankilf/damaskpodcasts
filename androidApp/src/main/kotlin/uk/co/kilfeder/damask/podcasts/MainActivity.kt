package uk.co.kilfeder.damask.podcasts

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uk.co.kilfeder.damask.podcasts.ui.App

/** The splash exit (design hand-off › Splash): the icon shrinks to 90% and fades as Home shows. */
private const val SPLASH_EXIT_MS = 200L
private const val SPLASH_EXIT_SCALE = 0.9f

class MainActivity : ComponentActivity() {
    /** Home has drawn its first frame (from the local database); the splash can go. */
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest launches with the splash theme; the app's own theme takes over from here.
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        holdSplashUntilReady()
        splashScreen.setOnExitAnimationListener { splash ->
            // No icon view when the launcher gave no animated icon (some devices): then just fade.
            splash.iconView?.animate()
                ?.scaleX(SPLASH_EXIT_SCALE)
                ?.scaleY(SPLASH_EXIT_SCALE)
                ?.alpha(0f)
                ?.setDuration(SPLASH_EXIT_MS)
                ?.start()
            splash.animate().alpha(0f).setDuration(SPLASH_EXIT_MS).withEndAction { splash.remove() }.start()
        }
        val graph = (application as DamaskApplication).graph
        setContent { App(graph, onReady = { ready = true }) }
    }

    /** Keeps the splash up until [ready], never for the network (Home draws from the database). */
    private fun holdSplashUntilReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return ready
                }
            },
        )
    }
}
