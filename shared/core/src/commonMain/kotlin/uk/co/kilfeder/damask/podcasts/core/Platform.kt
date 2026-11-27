package uk.co.kilfeder.damask.podcasts.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
