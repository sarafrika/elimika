-- Add missing id column to notification_delivery_log table
-- This fixes the schema mismatch between JPA entity expecting id column from BaseEntity
-- and the table having only uuid as primary key

-- Add the id column with auto-increment
ALTER TABLE notification_delivery_log ADD COLUMN id BIGSERIAL;

-- Drop the existing primary key constraint on uuid
ALTER TABLE notification_delivery_log DROP CONSTRAINT notification_delivery_log_pkey;

-- Make id the new primary key
ALTER TABLE notification_delivery_log ADD PRIMARY KEY (id);

-- Keep uuid column as unique but not primary key
ALTER TABLE notification_delivery_log ADD CONSTRAINT uk_notification_delivery_log_uuid UNIQUE (uuid);

-- Rename created_at and updated_at columns to match BaseEntity @Column names
-- BaseEntity uses: created_date, updated_date, created_by, updated_by
ALTER TABLE notification_delivery_log RENAME COLUMN created_at TO created_date;
ALTER TABLE notification_delivery_log RENAME COLUMN updated_at TO updated_date;

-- Column updated_by already matches BaseEntity @Column(name = "updated_by")