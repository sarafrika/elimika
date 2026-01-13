ALTER TABLE users
    ADD COLUMN user_no VARCHAR(9);

CREATE SEQUENCE user_no_seq
    MINVALUE 1
    MAXVALUE 99999999
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE OR REPLACE FUNCTION verhoeff_check_digit(input_text TEXT) RETURNS INT AS $$
DECLARE
    c INT := 0;
    i INT;
    digit INT;
    d_table INT[][] := ARRAY[
        ARRAY[0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
        ARRAY[1, 2, 3, 4, 0, 6, 7, 8, 9, 5],
        ARRAY[2, 3, 4, 0, 1, 7, 8, 9, 5, 6],
        ARRAY[3, 4, 0, 1, 2, 8, 9, 5, 6, 7],
        ARRAY[4, 0, 1, 2, 3, 9, 5, 6, 7, 8],
        ARRAY[5, 9, 8, 7, 6, 0, 4, 3, 2, 1],
        ARRAY[6, 5, 9, 8, 7, 1, 0, 4, 3, 2],
        ARRAY[7, 6, 5, 9, 8, 2, 1, 0, 4, 3],
        ARRAY[8, 7, 6, 5, 9, 3, 2, 1, 0, 4],
        ARRAY[9, 8, 7, 6, 5, 4, 3, 2, 1, 0]
    ];
    p_table INT[][] := ARRAY[
        ARRAY[0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
        ARRAY[1, 5, 7, 6, 2, 8, 3, 0, 9, 4],
        ARRAY[5, 8, 0, 3, 7, 9, 6, 1, 4, 2],
        ARRAY[8, 9, 1, 6, 0, 4, 3, 5, 2, 7],
        ARRAY[9, 4, 5, 3, 1, 2, 6, 8, 7, 0],
        ARRAY[4, 2, 8, 6, 5, 7, 3, 9, 0, 1],
        ARRAY[2, 7, 9, 3, 8, 0, 6, 4, 1, 5],
        ARRAY[7, 0, 4, 6, 9, 1, 3, 2, 5, 8]
    ];
    inv_table INT[] := ARRAY[0, 4, 3, 2, 1, 5, 6, 7, 8, 9];
BEGIN
    IF input_text IS NULL OR input_text = '' THEN
        RAISE EXCEPTION 'Input text is required for Verhoeff check digit';
    END IF;

    FOR i IN 0..length(input_text) - 1 LOOP
        digit := substring(input_text, length(input_text) - i, 1)::int;
        c := d_table[c + 1][p_table[(i % 8) + 1][digit + 1] + 1];
    END LOOP;

    RETURN inv_table[c + 1];
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION build_user_no(seq_value BIGINT) RETURNS VARCHAR AS $$
DECLARE
    base TEXT;
    check_digit INT;
BEGIN
    base := lpad(seq_value::text, 8, '0');
    check_digit := verhoeff_check_digit(base);
    RETURN base || check_digit::text;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

UPDATE users
SET user_no = build_user_no(nextval('user_no_seq'))
WHERE user_no IS NULL;

ALTER TABLE users
    ALTER COLUMN user_no SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_user_no_format CHECK (user_no ~ '^[0-9]{9}$');

CREATE UNIQUE INDEX idx_users_user_no ON users (user_no);

DROP FUNCTION IF EXISTS build_user_no(BIGINT);
DROP FUNCTION IF EXISTS verhoeff_check_digit(TEXT);
