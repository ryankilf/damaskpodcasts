package uk.co.kilfeder.damask.server.search

/** All configuration is environment variables (spec › Deploy); see deploy/.env.example. */
data class GatewayConfig(
    val port: Int,
    val supabaseUrl: String?,
    val meiliUrl: String,
    val meiliSearchKey: String?,
    val privateToken: String?,
    val piPassthrough: Boolean,
    val piKey: String?,
    val piSecret: String?,
    val version: String,
    /** The hostname Caddy serves, for the certificate check; unset in local compose. */
    val publicHostname: String?,
    /** Multiplies the requested region's `rankScore` (`lang=en-GB`); 1.0 = no boost. */
    val regionBoost: Double = MeiliSource.DEFAULT_REGION_BOOST,
) {
    companion object {
        private const val DEFAULT_PORT = 8080

        fun fromEnv(env: Map<String, String> = System.getenv()): GatewayConfig {
            fun opt(name: String) = env[name]?.trim()?.takeIf { it.isNotEmpty() }
            return GatewayConfig(
                port = opt("PORT")?.toInt() ?: DEFAULT_PORT,
                supabaseUrl = opt("SUPABASE_URL"),
                meiliUrl = opt("MEILI_URL") ?: "http://127.0.0.1:7700",
                meiliSearchKey = opt("MEILI_SEARCH_KEY"),
                privateToken = opt("PRIVATE_TOKEN"),
                piPassthrough = opt("PI_PASSTHROUGH") == "true",
                piKey = opt("PI_KEY"),
                piSecret = opt("PI_SECRET"),
                version = opt("DAMASK_VERSION") ?: "dev",
                publicHostname = opt("PUBLIC_HOSTNAME"),
                regionBoost = opt("REGION_BOOST")?.toDouble() ?: MeiliSource.DEFAULT_REGION_BOOST,
            )
        }
    }
}
