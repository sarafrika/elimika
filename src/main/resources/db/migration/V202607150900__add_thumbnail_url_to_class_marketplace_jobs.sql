-- Allow organisations to attach a thumbnail image to a marketplace class advert.
-- Stores the canonical media storage key (bare key), resolved to a URL on read.
ALTER TABLE class_marketplace_jobs
    ADD COLUMN thumbnail_url VARCHAR(500);

COMMENT ON COLUMN class_marketplace_jobs.thumbnail_url IS
    'Storage key for the class advert thumbnail image; resolved to a public URL by FileUrlResolver on read.';
