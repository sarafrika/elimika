ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(255),
    ADD COLUMN username          VARCHAR(50);

ALTER TABLE instructors
    ADD COLUMN professional_headline VARCHAR(255),
    ADD COLUMN lat                   DECIMAL(15, 12),
    ADD COLUMN long                  DECIMAL(15, 12),
    ADD COLUMN website               VARCHAR(255);

AlTER TABLE students
    ADD COLUMN bio TEXT;

ALTER TABLE organisation
    ADD COLUMN lat  DECIMAL(15, 12),
    ADD COLUMN long DECIMAL(15, 12);