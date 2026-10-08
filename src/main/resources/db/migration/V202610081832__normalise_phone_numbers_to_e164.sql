-- Rewrites stored phone numbers as E.164; local and 254-prefixed Kenyan numbers gain +254.
-- Values that cannot be resolved are left unchanged.

CREATE FUNCTION pg_temp.to_e164(raw TEXT) RETURNS TEXT AS
$$
DECLARE
    cleaned TEXT := regexp_replace(raw, '[[:space:]().-]', '', 'g');
BEGIN
    IF cleaned ~ '^00[1-9]' THEN
        cleaned := '+' || substr(cleaned, 3);
    END IF;
    IF cleaned ~ '^(\+?254|0)?[17][0-9]{8}$' THEN
        RETURN '+254' || right(cleaned, 9);
    END IF;
    IF cleaned ~ '^\+[1-9][0-9]{6,14}$' THEN
        RETURN cleaned;
    END IF;
    RETURN raw;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

UPDATE users SET phone_number = pg_temp.to_e164(phone_number)
WHERE phone_number IS NOT NULL AND phone_number <> pg_temp.to_e164(phone_number);

UPDATE students SET guardian_1_mobile = pg_temp.to_e164(guardian_1_mobile)
WHERE guardian_1_mobile IS NOT NULL AND guardian_1_mobile <> pg_temp.to_e164(guardian_1_mobile);

UPDATE students SET guardian_2_mobile = pg_temp.to_e164(guardian_2_mobile)
WHERE guardian_2_mobile IS NOT NULL AND guardian_2_mobile <> pg_temp.to_e164(guardian_2_mobile);

UPDATE organisation_invitations SET guardian_phone = pg_temp.to_e164(guardian_phone)
WHERE guardian_phone IS NOT NULL AND guardian_phone <> pg_temp.to_e164(guardian_phone);

UPDATE student_guardian_contacts SET guardian_phone = pg_temp.to_e164(guardian_phone)
WHERE guardian_phone IS NOT NULL AND guardian_phone <> pg_temp.to_e164(guardian_phone);

UPDATE training_branches SET poc_telephone = pg_temp.to_e164(poc_telephone)
WHERE poc_telephone IS NOT NULL AND poc_telephone <> pg_temp.to_e164(poc_telephone);
