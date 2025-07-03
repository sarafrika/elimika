-- 202507021535__create_categories_table.sql
-- Create categories table for organizing courses

CREATE TABLE course_categories
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name         VARCHAR(100)             NOT NULL UNIQUE,
    description  TEXT,
    parent_uuid  UUID REFERENCES course_categories (uuid),
    is_active    BOOLEAN                                  DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by   VARCHAR(255)             NOT NULL,
    updated_by   VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_categories_uuid ON categories (uuid);
CREATE INDEX idx_categories_parent_uuid ON categories (parent_uuid);
CREATE INDEX idx_categories_name ON categories (name);
CREATE INDEX idx_categories_created_date ON categories (created_date);