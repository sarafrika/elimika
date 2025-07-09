-- Migration to create training center branches table
-- V202507092058__CreateTrainingCenterBranchesTable.sql

-- Training Branches Table (extends organisation for multiple locations)
CREATE TABLE IF NOT EXISTS training_branches
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID         NOT NULL REFERENCES organisation(uuid),
    branch_name       VARCHAR(200) NOT NULL,
    address           TEXT,
    poc_user_uuid     UUID         REFERENCES users(uuid), -- Point of Contact User UUID
    active            BOOLEAN      NOT NULL        DEFAULT true,
    created_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN      NOT NULL        DEFAULT FALSE
);

-- Add additional fields to organisation table for training center specific info
ALTER TABLE organisation
    ADD COLUMN IF NOT EXISTS user_uuid UUID REFERENCES users(uuid),
    ADD COLUMN IF NOT EXISTS location VARCHAR(200),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Create indices for performance
CREATE INDEX idx_training_branches_organisation_uuid ON training_branches (organisation_uuid);
CREATE INDEX idx_training_branches_active ON training_branches (active);
CREATE INDEX idx_training_branches_branch_name ON training_branches (branch_name);
CREATE UNIQUE INDEX idx_training_branches_org_branch ON training_branches (organisation_uuid, branch_name) WHERE deleted = FALSE;