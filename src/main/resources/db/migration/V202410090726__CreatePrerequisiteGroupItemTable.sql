CREATE TABLE IF NOT EXISTS prerequisite_group_item
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    prerequisite_group_id BIGINT      NOT NULL,
    prerequisite_id       BIGINT      NOT NULL,
    created_date          TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50) NOT NULL,
    updated_date          TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    deleted               BOOLEAN     NOT NULL        DEFAULT FALSE,

    FOREIGN KEY (prerequisite_group_id) REFERENCES prerequisite_group (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (prerequisite_id) REFERENCES prerequisite (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_prerequisite_group_item_prerequisite_group_items ON prerequisite_group_item (prerequisite_group_id, prerequisite_id);
CREATE INDEX idx_prerequisite_group_item_created_by ON prerequisite_group_item (created_by);
CREATE INDEX idx_prerequisite_group_item_updated_by ON prerequisite_group_item (updated_by);
CREATE INDEX idx_prerequisite_group_item_deleted ON prerequisite_group_item (deleted);