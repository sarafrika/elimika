-- Audio lesson uploads were rejected because the seeded MIME list used the
-- non-standard "audio/mp3"; browsers report MP3 files as "audio/mpeg".
-- Broaden the Audio content type to the standard/common audio MIME types.
UPDATE lesson_content_types
SET mime_types = ARRAY [
        'audio/mpeg',
        'audio/mp3',
        'audio/wav',
        'audio/x-wav',
        'audio/ogg',
        'audio/mp4',
        'audio/x-m4a',
        'audio/aac',
        'audio/webm'
    ]
WHERE name = 'Audio';
