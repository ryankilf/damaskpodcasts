# Deploying Damask search

One small VPS runs everything in [docker-compose.yml](docker-compose.yml) (spec › Search):

| Service | Does | Reachable from |
|---|---|---|
| `caddy` | Terminates TLS (automatic Let's Encrypt certificates, ZeroSSL as the fallback issuer) and reverse-proxies `/v1/*` and `/health` to the gateway | The internet, ports 80 and 443 |
| `gateway` | The Ktor search API (`server/search`) | The private compose network only |
| `meilisearch` | The index, built from the Podcast Index dump | The private compose network only |
| `importer` | Weekly one-shot job (`server/importer`) | Run by a systemd timer |

Nothing here is deployed automatically: these are the files and the steps.

## Run it locally first

Requires Docker and a JDK 21.

```sh
cd deploy
./build.sh                       # Gradle builds the two distributions, then the images
docker compose --env-file local.env -f docker-compose.yml -f docker-compose.local.yml up
```

That starts Meilisearch, loads [fixtures/podcasts.sql](fixtures/podcasts.sql) (12 rows, 6 kept), and serves the gateway on `localhost:8080` without Caddy:

```sh
curl http://localhost:8080/health
curl -H 'Authorization: Bearer local-dev-private-token' 'http://localhost:8080/v1/search?q=hardcore%20histry'
```

`local.env` holds throwaway dev-only values. Never use them on a server.

## Server setup

1. **DNS.** In Cloudflare, an `A` (and `AAAA`) record for the search hostname pointing at the VPS, **DNS only (grey cloud)**. TLS terminates on the VPS, not at Cloudflare.
2. **Firewall.** Allow inbound 80 (ACME challenges and the HTTP→HTTPS redirect) and 443 (TCP and UDP for HTTP/3), plus SSH. Everything else is denied. SSH is key-only (`PasswordAuthentication no`). Neither Meilisearch nor the gateway publishes a host port.
3. **Docker** and the compose plugin, from Docker's own repository.
4. **The repository** at `/opt/damask` (the systemd unit assumes this path), then:
   ```sh
   cd /opt/damask/deploy
   cp .env.example .env && chmod 600 .env
   # fill in PUBLIC_HOSTNAME, ACME_EMAIL, MEILI_MASTER_KEY, MEILI_SEARCH_KEY_UID, SUPABASE_URL, PRIVATE_TOKEN
   ./search-key.sh      # prints the derived key; put it in .env as MEILI_SEARCH_KEY
   ./build.sh
   docker compose --env-file .env up -d meilisearch
   docker compose --env-file .env --profile import run --rm importer   # first import: downloads ~1.8 GB
   docker compose --env-file .env --profile public up -d
   ```
5. **The weekly import:**
   ```sh
   sudo cp systemd/damask-import.* /etc/systemd/system/
   sudo systemctl daemon-reload && sudo systemctl enable --now damask-import.timer
   systemctl list-timers damask-import.timer
   ```

### The search key

The gateway never holds the master key. It uses a Meilisearch key restricted to the `search` action on the `podcasts` index. Meilisearch derives a key's value as `HMAC-SHA256(master key, key UID)`, so:

- the importer creates (or confirms) the key with the fixed `MEILI_SEARCH_KEY_UID` on every run;
- `./search-key.sh` computes the same value for `.env`, so the key is never printed from the server.

`MeiliClientTest` asserts this derivation, so a Meilisearch upgrade that changes it fails CI before it can break a deploy.

### Never enable Caddy's access log

Caddy's access log records the full request URI, including the query string, which would write users' search text to disk. The [Caddyfile](Caddyfile) has no `log` directive for that reason. The gateway keeps its own access log: method, path (no query string), status, latency and a 12-character hash of the caller's ID.

## Monitoring

Use any external uptime service that can email you:

- Monitor `https://<PUBLIC_HOSTNAME>/health`.
- Alert on any non-200 response. `/health` returns 503 when the index is missing or Meilisearch is down.
- Add a **keyword alert on `warning`**: the body reports `"status":"warning"` when the certificate has 14 days or fewer left (or has expired or can't be read), while still returning 200.
- If the service has its own certificate-expiry check, turn that on too.

`/health` returns `{"status":"ok"|"warning"|"down","db":true|false,"cert":"ok"|"warning"|"expired"|"unknown"}`. The certificate check is cached for an hour.

## Abuse

Limits are per user (the Supabase token's `sub`): search allows bursts of 20, refilling at 1 a second. There is deliberately no per-IP limit, because shared IPs (mobile carriers, offices) would knock out legitimate users.

Anonymous Supabase sessions are cheap to create, which is the obvious way around per-user limits. Supabase rate-limits anonymous sign-ins per IP in its Auth settings, and can put a CAPTCHA (Cloudflare Turnstile or hCaptcha) in front of them. If the gateway is still abused, the documented fallback is: an IP-based limit for anonymous tokens (`is_anonymous: true`), and the per-user limit for signed-in ones.

## Updating

```sh
cd /opt/damask && git pull && cd deploy && ./build.sh
docker compose --env-file .env --profile public up -d
```

To upgrade Meilisearch, change the tag in `docker-compose.yml` and `meilisearch` in `gradle/libs.versions.toml` in the same commit (the tests use the catalog's version; CI fails if the two differ). Meilisearch upgrades can need a dump-and-reimport; the weekly import rebuilds the index from scratch anyway.
