package uk.co.kilfeder.damask.podcasts.core.directory

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppleDirectoryTest {
    private val reply = """
        {"resultCount":4,"results":[
          {"collectionId":1,"collectionName":"History Hour","artistName":"BBC","feedUrl":"https://a.example/feed",
           "artworkUrl100":"https://img/100.jpg","artworkUrl600":"https://img/600.jpg","collectionExplicitness":"notExplicit"},
          {"collectionId":2,"collectionName":"No Feed","artistName":"Nobody"},
          {"collectionId":3,"trackName":"Rude Show","artistName":"Someone","feedUrl":"http://b.example/rss/",
           "collectionExplicitness":"explicit"},
          {"collectionId":4,"collectionName":"Rude Show (again)","feedUrl":"https://B.example/rss"}
        ]}
    """.trimIndent()

    private fun directory(status: HttpStatusCode = HttpStatusCode.OK, seen: MutableList<HttpRequestData>? = null) =
        AppleDirectory(
            HttpClient(
                MockEngine { request ->
                    seen?.add(request)
                    respond(if (status == HttpStatusCode.OK) reply else "", status)
                },
            ) { expectSuccess = false },
        )

    @Test
    fun keepsOnlyShowsWithAFeedEachFeedOnce() = runTest {
        val found = assertIs<DirectoryResult.Found>(directory().search("history", "GB", hideExplicit = false))
        assertEquals(listOf("History Hour", "Rude Show"), found.shows.map { it.title })
        assertEquals("https://img/600.jpg", found.shows.first().artworkUrl)
        assertEquals(true, found.shows[1].explicit)
    }

    @Test
    fun hideExplicitAsksAppleAndFiltersToo() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        val result = directory(seen = seen).search(" history ", "GB", hideExplicit = true)
        val found = assertIs<DirectoryResult.Found>(result)
        assertEquals(listOf("History Hour"), found.shows.map { it.title })
        val url = seen.single().url
        assertEquals("No", url.parameters["explicit"])
        assertEquals("gb", url.parameters["country"])
        assertEquals("history", url.parameters["term"])
        assertEquals("podcast", url.parameters["media"])
    }

    @Test
    fun noRegionMeansNoCountry() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        directory(seen = seen).search("history", null, hideExplicit = false)
        assertEquals(null, seen.single().url.parameters["country"])
    }

    @Test
    fun refusalIsBusy() = runTest {
        assertIs<DirectoryResult.Busy>(directory(HttpStatusCode.Forbidden).search("x", "GB", false))
    }

    @Test
    fun otherErrorsFail() = runTest {
        assertIs<DirectoryResult.Failed>(directory(HttpStatusCode.NotFound).search("x", "GB", false))
    }
}
