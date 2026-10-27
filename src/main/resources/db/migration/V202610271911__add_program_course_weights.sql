-- Mandatory programme courses carry a share of the programme mark; optional ones carry none.
ALTER TABLE program_courses
    ADD COLUMN IF NOT EXISTS weight_percentage NUMERIC(5, 2);

ALTER TABLE program_courses
    DROP CONSTRAINT IF EXISTS chk_program_courses_weight;
ALTER TABLE program_courses
    ADD CONSTRAINT chk_program_courses_weight
        CHECK (weight_percentage IS NULL OR (weight_percentage > 0 AND weight_percentage <= 100));

-- Existing programmes get an even split so they stay publishable; the last course absorbs rounding.
WITH ranked AS (SELECT uuid,
                       ROW_NUMBER() OVER (PARTITION BY program_uuid ORDER BY sequence_order, id) AS position,
                       COUNT(*) OVER (PARTITION BY program_uuid)                                 AS total
                FROM program_courses
                WHERE is_required IS DISTINCT FROM false)
UPDATE program_courses pc
SET weight_percentage = CASE
                            WHEN ranked.position < ranked.total THEN TRUNC(100.0 / ranked.total, 2)
                            ELSE 100 - TRUNC(100.0 / ranked.total, 2) * (ranked.total - 1)
    END
FROM ranked
WHERE pc.uuid = ranked.uuid
  AND pc.weight_percentage IS NULL;

ALTER TABLE program_enrollments
    ADD COLUMN IF NOT EXISTS final_grade_provisional BOOLEAN NOT NULL DEFAULT true;

UPDATE program_enrollments
SET final_grade_provisional = false
WHERE UPPER(result_status) IN ('PASSED', 'FAILED');
