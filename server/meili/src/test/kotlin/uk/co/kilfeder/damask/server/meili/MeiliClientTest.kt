package uk.co.kilfeder.damask.server.meili

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeiliClientTest {

    @Test
    fun languagePrefixIsThePrimarySubtag() {
        assertEquals("en", PodcastIndex.languagePrefix("en-GB"))
        assertEquals("pt", PodcastIndex.languagePrefix(" PT_br "))
        assertEquals("", PodcastIndex.languagePrefix(""))
    }

    @Test
    fun indexLifecycleAgainstRealMeilisearch(): Unit = runBlocking {
        val meili = MeiliContainer.masterClient()
        val admin = MeiliAdmin(meili)
        assertTrue(meili.isHealthy())
        val index = "client_test"
        meili.awaitTask(admin.createIndex(index))
        meili.awaitTask(admin.updateSettings(index, PodcastIndex.settings))
        val doc = PodcastDocument(id = 1, title = "Hardcore History", feedUrl = "https://e.com/f", dedupeKey = "id:1")
        val ndjson = MeiliClient.json.encodeToString(PodcastDocument.serializer(), doc)
        meili.awaitTask(admin.addDocumentsNdjson(index, ndjson))
        assertEquals(1, meili.documentCount(index))
        assertEquals(listOf(doc), meili.search(index, MeiliQuery("hardcore", limit = 5, offset = 0)).hits)
        meili.awaitTask(admin.deleteIndex(index))
        assertTrue(!meili.indexExists(index))
    }

    @Test
    fun searchOnlyKeyCanSearchButNotWrite(): Unit = runBlocking {
        val master = MeiliContainer.masterClient()
        val admin = MeiliAdmin(master)
        if (!master.indexExists(PodcastIndex.LIVE)) master.awaitTask(admin.createIndex(PodcastIndex.LIVE))
        val key = admin.ensureKey(
            uid = "6f2d7c9e-1a3b-4c5d-8e9f-0a1b2c3d4e5f",
            description = "gateway search",
            actions = listOf("search"),
            indexes = listOf(PodcastIndex.LIVE),
        )
        val again = admin.ensureKey(key.uid, "gateway search", listOf("search"), listOf(PodcastIndex.LIVE))
        assertEquals(key, again, "idempotent")
        // deploy/ relies on this: the key's value is HMAC-SHA256(master key, uid), so .env only
        // needs the UID and setup derives the value. Fails if a Meilisearch upgrade changes it.
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(MeiliContainer.MASTER_KEY.toByteArray(), "HmacSHA256"))
        assertEquals(mac.doFinal(key.uid.toByteArray()).joinToString("") { "%02x".format(it) }, key.key)
        val searchOnly = MeiliClient.create(MeiliContainer.require(), key.key, timeoutMs = 5_000)
        searchOnly.search(PodcastIndex.LIVE, MeiliQuery("x", limit = 1, offset = 0))
        assertFailsWith<MeiliException> { MeiliAdmin(searchOnly).createIndex("not_allowed") }
    }
}
