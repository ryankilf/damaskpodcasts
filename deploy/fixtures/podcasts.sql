-- A tiny stand-in for the Podcast Index dump (same `podcasts` schema as podcastindex_feeds.db,
-- checked against the 2026-11-21 dump). Used by the importer tests, the gateway's integration
-- tests and `docker compose` locally. Each row exists to exercise one rule; see the comments.
-- Timestamps are epoch seconds; "recent" means 2026, "old" means 2019.
-- duplicateOf: the real dump stores "not a duplicate" as '' (text), so most rows here do too;
-- row 2 uses NULL and row 12 uses 0, so every spelling of "none" stays covered.
CREATE TABLE podcasts (
    id INTEGER PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    lastUpdate INTEGER,
    link TEXT NOT NULL,
    lastHttpStatus INTEGER,
    dead INTEGER,
    contentType TEXT NOT NULL,
    itunesId INTEGER,
    originalUrl TEXT NOT NULL,
    itunesAuthor TEXT NOT NULL,
    itunesOwnerName TEXT NOT NULL,
    explicit INTEGER,
    imageUrl TEXT NOT NULL,
    itunesType TEXT NOT NULL,
    generator TEXT NOT NULL,
    newestItemPubdate INTEGER,
    language TEXT NOT NULL,
    oldestItemPubdate INTEGER,
    episodeCount INTEGER,
    popularityScore INTEGER,
    priority INTEGER,
    createdOn INTEGER,
    updateFrequency INTEGER,
    chash TEXT NOT NULL,
    host TEXT NOT NULL,
    newestEnclosureUrl TEXT NOT NULL,
    podcastGuid TEXT NOT NULL,
    description TEXT NOT NULL,
    category1 TEXT NOT NULL,
    category2 TEXT NOT NULL,
    category3 TEXT NOT NULL,
    category4 TEXT NOT NULL,
    category5 TEXT NOT NULL,
    category6 TEXT NOT NULL,
    category7 TEXT NOT NULL,
    category8 TEXT NOT NULL,
    category9 TEXT NOT NULL,
    category10 TEXT NOT NULL,
    newestEnclosureDuration INTEGER,
    podcastId INTEGER,
    duplicateOf INTEGER
);

-- 1 KEPT: popular, English, with a podcast GUID and categories (one repeated).
INSERT INTO podcasts VALUES (1, 'https://feeds.example.com/hardcore-history', 'Dan Carlin''s Hardcore History', 0, '', 200, 0, '', 0, 'https://old.example.com/hh', 'Dan Carlin', 'Dan Carlin Owner', 0, 'https://img.example.com/hh.jpg', 'episodic', '', 1795000000, 'en-US', 1100000000, 70, 5, 0, 0, 0, 'chash-hh', '', '', 'guid-hardcore-history', 'Long description is not indexed', 'History', 'Society', 'History', '', '', '', '', '', '', '', 0, 0, '');
-- 2 KEPT, then COLLAPSED at query time: same podcast GUID as 1 under another feed URL. duplicateOf NULL.
INSERT INTO podcasts VALUES (2, 'https://mirror.example.com/hh', 'Hardcore History (mirror)', 0, '', 200, 0, '', 0, 'https://mirror.example.com/hh', 'Dan Carlin', '', 0, '', 'episodic', '', 1794000000, 'en', 0, 70, 1, 0, 0, 0, 'chash-mirror', '', '', 'guid-hardcore-history', '', 'History', '', '', '', '', '', '', '', '', '', 0, 0, NULL);
-- 3 DROPPED: dead.
INSERT INTO podcasts VALUES (3, 'https://dead.example.com/feed', 'Dead History Show', 0, '', 200, 1, '', 0, 'https://dead.example.com/feed', 'Nobody', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 10, 3, 0, 0, 0, 'chash-dead', '', '', '', '', 'History', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 4 DROPPED: a duplicate of feed 1.
INSERT INTO podcasts VALUES (4, 'https://dupe.example.com/feed', 'Hardcore History Duplicate', 0, '', 200, 0, '', 0, 'https://dupe.example.com/feed', 'Dan Carlin', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 70, 3, 0, 0, 0, 'chash-dupe', '', '', '', '', 'History', '', '', '', '', '', '', '', '', '', 0, 0, 1);
-- 5 DROPPED: no episodes.
INSERT INTO podcasts VALUES (5, 'https://empty.example.com/feed', 'Empty Show', 0, '', 200, 0, '', 0, 'https://empty.example.com/feed', 'Someone', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 0, 3, 0, 0, 0, 'chash-empty', '', '', '', '', 'News', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 6 DROPPED: last fetch was a 404.
INSERT INTO podcasts VALUES (6, 'https://gone.example.com/feed', 'Gone Show', 0, '', 404, 0, '', 0, 'https://gone.example.com/feed', 'Someone', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 50, 3, 0, 0, 0, 'chash-gone', '', '', '', '', 'News', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 7 DROPPED: junk, popularity 0 with fewer than 3 episodes.
INSERT INTO podcasts VALUES (7, 'https://junk.example.com/feed', 'Junk Two Episodes', 0, '', 200, 0, '', 0, 'https://junk.example.com/feed', 'Someone', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 2, 0, 0, 0, 0, 'chash-junk', '', '', '', '', 'News', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 8 DROPPED: junk, popularity 0 with nothing newer than 3 years.
INSERT INTO podcasts VALUES (8, 'https://stale.example.com/feed', 'Stale Show', 0, '', 200, 0, '', 0, 'https://stale.example.com/feed', 'Someone', '', 0, '', 'episodic', '', 1560000000, 'en', 0, 40, 0, 0, 0, 0, 'chash-stale', '', '', '', '', 'News', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 9 KEPT: popularity 0 but recent with enough episodes; French; explicit; serial; no GUID, so the dedupe key is its chash.
INSERT INTO podcasts VALUES (9, 'https://fr.example.com/feed', 'Histoire Explicite', 0, '', 200, 0, '', 0, 'https://fr.example.com/feed', 'Auteur', 'Propriétaire', 1, '', 'serial', '', 1795000000, 'fr', 0, 12, 0, 0, 0, 0, 'chash-fr', '', '', '', '', 'History', 'Comedy', '', '', '', '', '', '', '', '', 0, 0, '');
-- 10 KEPT: same chash as 9 (no GUIDs): collapsed with 9 at query time.
INSERT INTO podcasts VALUES (10, 'https://fr-mirror.example.com/feed', 'Histoire Explicite (miroir)', 0, '', 200, 0, '', 0, 'https://fr-mirror.example.com/feed', 'Auteur', '', 1, '', 'serial', '', 1795000000, 'fr-FR', 0, 12, 0, 0, 0, 0, 'chash-fr', '', '', '', '', 'History', '', '', '', '', '', '', '', '', '', 0, 0, '');
-- 11 KEPT: no GUID and no chash, so the dedupe key falls back to "id:11"; explicit English tech show.
INSERT INTO podcasts VALUES (11, 'https://tech.example.com/feed', 'Explicit Tech Talk', 0, '', 200, 0, '', 0, 'https://tech.example.com/feed', 'Techie', '', 1, 'https://img.example.com/tech.jpg', 'episodic', '', 1795000000, 'en-gb', 0, 30, 2, 0, 0, 0, '', '', '', '', '', 'Technology', 'News', '', '', '', '', '', '', '', '', 0, 0, '');
-- 12 KEPT: duplicateOf = 0 means "not a duplicate" in some dumps; treated like NULL.
INSERT INTO podcasts VALUES (12, 'https://history-extra.example.com/feed', 'History Extra', 0, '', 200, 0, '', 0, 'https://history-extra.example.com/feed', 'Immediate Media', '', 0, '', 'episodic', '', 1795000000, 'en', 0, 900, 4, 0, 0, 0, 'chash-he', '', '', 'guid-history-extra', '', 'History', '', '', '', '', '', '', '', '', '', 0, 0, 0);
