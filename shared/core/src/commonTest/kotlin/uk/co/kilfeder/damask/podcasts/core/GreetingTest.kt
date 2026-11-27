package uk.co.kilfeder.damask.podcasts.core

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {

    @Test
    fun sayHelloFormatsName() {
        assertEquals("Hello, Damask!", sayHello("Damask"))
    }
}
