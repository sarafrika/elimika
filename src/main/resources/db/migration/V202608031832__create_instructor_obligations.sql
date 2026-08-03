-- Instructor obligations: what an organisation owes an instructor, as a persisted record.
--
-- Until now this figure existed only as an expression evaluated on every page load:
-- `training_fee x completed_session_count`, grouped by the class' default instructor. Nothing was
-- stored, so there was no obligation, no state, no settlement, no history and nothing to audit. Two
-- consequences made that untenable. First, the number silently rewrote history — the multiplication
-- used the *current* training_fee, so re-rating a class retroactively changed what the organisation
-- had owed for sessions delivered months earlier. Second, there was nowhere to record that the money
-- had actually been paid, so an organisation that settled with an instructor had no way to say so and
-- the platform kept billing it forever.
--
-- One row here is one delivered session's worth of pay for one instructor. `rate_amount` is
-- snapshotted at accrual and never recomputed: that is the entire point of the table. Changing a
-- class' training_fee from tomorrow changes tomorrow's obligations and leaves yesterday's alone.
--
-- Settlement is deliberately OFF-PLATFORM ("Mode A"). The organisation pays the instructor by its own
-- means — bank transfer, mobile money, payroll — and then records that fact here with a reference.
-- No money moves through a wallet: the platform holds no organisation float and has no basis to take
-- custody of an organisation's payroll. The columns are a strict subset of what an on-platform
-- settlement would need, so moving to that later is additive rather than a rewrite.
--
-- NO BACKFILL. Sessions completed before this migration do not become obligations. We have no
-- trustworthy historical rate to snapshot for them — only today's training_fee, which is exactly the
-- number this table exists to stop trusting — and inventing one would fabricate debts that no
-- organisation ever agreed to. Accrual starts from the first session that completes after deploy.
-- The organisation payables screen will therefore read zero on day one; that is honest, not a bug.

CREATE TABLE instructor_obligations
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    organisation_uuid     UUID                     NOT NULL,
    instructor_uuid       UUID                     NOT NULL,
    instructor_user_uuid  UUID,
    class_definition_uuid UUID                     NOT NULL,
    session_uuid          UUID                     NOT NULL,

    rate_amount           NUMERIC(18, 4)           NOT NULL,
    currency_code         VARCHAR(3)               NOT NULL,
    status                VARCHAR(16)              NOT NULL        DEFAULT 'ACCRUED',

    accrued_at            TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    settled_at            TIMESTAMP WITH TIME ZONE,
    settlement_reference  VARCHAR(128),
    settled_by            VARCHAR(255),
    status_note           TEXT,

    created_date          TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date          TIMESTAMP WITH TIME ZONE,
    created_by            VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by            VARCHAR(255),

    CONSTRAINT fk_instructor_obligations_currency
        FOREIGN KEY (currency_code) REFERENCES currencies (code),

    -- One session pays one instructor once. This is the constraint that makes accrual safe to retry:
    -- the listener pre-checks, but a concurrent redelivery of the same completion event is stopped
    -- here rather than by hope.
    CONSTRAINT uq_instructor_obligations_session
        UNIQUE (class_definition_uuid, session_uuid, instructor_uuid),

    CONSTRAINT chk_instructor_obligations_status
        CHECK (status IN ('ACCRUED', 'SETTLED', 'CANCELLED', 'DISPUTED')),
    CONSTRAINT chk_instructor_obligations_rate_non_negative
        CHECK (rate_amount >= 0),
    CONSTRAINT chk_instructor_obligations_currency_length
        CHECK (char_length(currency_code) = 3),

    -- A settled row must say when and against what. Without this a settlement could be recorded as a
    -- bare status flip with no evidence, which is the failure mode the whole table exists to prevent.
    CONSTRAINT chk_instructor_obligations_settlement_evidence
        CHECK (status <> 'SETTLED'
            OR (settled_at IS NOT NULL AND settlement_reference IS NOT NULL AND settled_by IS NOT NULL))
);

-- The organisation payables screen aggregates by organisation, then by instructor, and cares mostly
-- about the unsettled rows.
CREATE INDEX idx_instructor_obligations_organisation_status
    ON instructor_obligations (organisation_uuid, status);
-- An instructor's own statement is read by user, across every organisation that engages them.
CREATE INDEX idx_instructor_obligations_instructor_user
    ON instructor_obligations (instructor_user_uuid);
CREATE INDEX idx_instructor_obligations_instructor
    ON instructor_obligations (instructor_uuid);
CREATE INDEX idx_instructor_obligations_class_definition
    ON instructor_obligations (class_definition_uuid);

COMMENT ON TABLE instructor_obligations IS
    'One delivered session''s pay owed by an organisation to an instructor, with the rate snapshotted at accrual and settled off-platform against a reference.';
COMMENT ON COLUMN instructor_obligations.organisation_uuid IS
    'The organisation that owes the money — copied from the class definition at accrual so a class changing hands cannot move an existing debt.';
COMMENT ON COLUMN instructor_obligations.instructor_uuid IS
    'Instructor profile owed, as carried on class_definitions.default_instructor_uuid. This is what the organisation payables screen groups by.';
COMMENT ON COLUMN instructor_obligations.instructor_user_uuid IS
    'The instructor''s platform user, resolved at accrual, so the instructor can read their own statement. Nullable because an unresolvable user must not block the debt from being recorded.';
COMMENT ON COLUMN instructor_obligations.session_uuid IS
    'The scheduled instance whose completion caused the obligation. Intentionally has no foreign key: a financial record must outlive the operational row that caused it.';
COMMENT ON COLUMN instructor_obligations.rate_amount IS
    'The per-session training fee AS IT STOOD when the session completed. Never recomputed — re-rating a class must not rewrite what was already earned.';
COMMENT ON COLUMN instructor_obligations.status IS
    'ACCRUED (owed, unpaid) | SETTLED (paid off-platform, evidenced) | CANCELLED (should never have accrued) | DISPUTED (contested, excluded from what is owed until resolved).';
COMMENT ON COLUMN instructor_obligations.settlement_reference IS
    'The organisation''s own reference for the payment it made — bank reference, mobile money code, payroll run id. The platform does not move the money, so this is the only evidence it holds.';
COMMENT ON COLUMN instructor_obligations.settled_by IS
    'The user who recorded the settlement, so a disputed payment has a name attached to it.';
COMMENT ON COLUMN instructor_obligations.status_note IS
    'Free-text reason accompanying a settlement, cancellation or dispute.';
