-- Adults may now learn as students. Lift the seeded upper bound of the student onboarding
-- age gate from 18 to 120, but only on the GLOBAL rule whose payload is still the original
-- seed: an administrator's customised value is left untouched.
UPDATE system_rules
SET value_payload = jsonb_set(value_payload, '{maxAge}', '120'::jsonb),
    updated_date  = CURRENT_TIMESTAMP,
    updated_by    = 'system'
WHERE rule_category = 'AGE_GATE'
  AND rule_key = 'student.onboarding.age_gate'
  AND rule_scope = 'GLOBAL'
  AND value_payload = '{"minAge":5,"maxAge":18}'::jsonb;
