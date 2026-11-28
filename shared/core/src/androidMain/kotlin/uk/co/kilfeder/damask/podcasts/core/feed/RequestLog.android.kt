package uk.co.kilfeder.damask.podcasts.core.feed

import android.util.Log

/** Filter Logcat by the tag `DamaskHttp`. */
internal actual fun writeHttpLog(line: String) {
    Log.d("DamaskHttp", line)
}
