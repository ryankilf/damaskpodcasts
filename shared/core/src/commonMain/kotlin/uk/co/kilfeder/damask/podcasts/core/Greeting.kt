package uk.co.kilfeder.damask.podcasts.core

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}
