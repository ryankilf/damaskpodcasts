package uk.co.kilfeder.damask.server.meili

import org.junit.Assume
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * One Meilisearch container per test JVM, pinned to the same version as deploy/ (passed in by
 * Gradle as `damask.meili.image`). Tests that need it call [require], which skips them when Docker
 * isn't available rather than failing.
 */
object MeiliContainer {
    const val MASTER_KEY = "test-master-key-0123456789abcdef"
    private const val PORT = 7700

    private val container: GenericContainer<*> by lazy {
        val image = requireNotNull(System.getProperty("damask.meili.image")) {
            "damask.meili.image is set by Gradle from the version catalog"
        }
        GenericContainer(DockerImageName.parse(image))
            .withExposedPorts(PORT)
            .withEnv("MEILI_MASTER_KEY", MASTER_KEY)
            .withEnv("MEILI_ENV", "development")
            .withEnv("MEILI_NO_ANALYTICS", "true")
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .also { it.start() }
    }

    fun require(): String {
        Assume.assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        return "http://${container.host}:${container.getMappedPort(PORT)}"
    }

    fun masterClient(timeoutMs: Long = 30_000): MeiliClient = MeiliClient.create(require(), MASTER_KEY, timeoutMs)

    fun masterAdmin(): MeiliAdmin = MeiliAdmin(masterClient())
}
