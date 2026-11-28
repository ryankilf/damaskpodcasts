package uk.co.kilfeder.damask.podcasts.core.feed

internal actual fun writeHttpLog(line: String) {
    println("[http] $line")
}
