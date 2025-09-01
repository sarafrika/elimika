-- Fix user_notification_preferences table to match BaseEntity schema expectations
-- This fixes the schema mismatch between JPA entity expecting id column from BaseEntity
-- and the table having only uuid as primary key

-- Add the id column with auto-increment
ALTER TABLE user_notification_preferences ADD COLUMN id BIGSERIAL;

-- Drop the existing primary key constraint on uuid  
ALTER TABLE user_notification_preferences DROP CONSTRAINT user_notification_preferences_pkey;

-- Make id the new primary key
ALTER TABLE user_notification_preferences ADD PRIMARY KEY (id);

-- Keep uuid column as unique but not primary key
ALTER TABLE user_notification_preferences ADD CONSTRAINT uk_user_notification_preferences_uuid UNIQUE (uuid);

-- Rename created_at and updated_at columns to match BaseEntity @Column names
-- BaseEntity uses: created_date, updated_date, created_by, updated_by
ALTER TABLE user_notification_preferences RENAME COLUMN created_at TO created_date;
ALTER TABLE user_notification_preferences RENAME COLUMN updated_at TO updated_date;

-- Column updated_by already matches BaseEntity @Column(name = "updated_by")