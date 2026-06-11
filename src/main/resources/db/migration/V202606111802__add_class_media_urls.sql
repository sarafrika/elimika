ALTER TABLE class_definitions
    ADD COLUMN thumbnail_url VARCHAR(500),
    ADD COLUMN promotional_video_url VARCHAR(500);

COMMENT ON COLUMN class_definitions.thumbnail_url IS 'Optional thumbnail image URL for class listings and previews';
COMMENT ON COLUMN class_definitions.promotional_video_url IS 'Optional promotional video URL for class marketing and previews';
