-- The JPA converters bind these columns as varchar, and staging's JDBC URL has no
-- stringtype=unspecified, so PostgreSQL enum columns reject the writes ("column is of type X but
-- expression is of type character varying"). Store them as varchar guarded by CHECK constraints,
-- matching the uppercase values the converters write, and drop the enum types.
-- Every step is guarded so the migration is a no-op on a column that is already varchar.

DO
$$
BEGIN
    -- users.gender (gender)
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'users'
                 AND column_name = 'gender' AND data_type = 'USER-DEFINED') THEN
        ALTER TABLE users ALTER COLUMN gender TYPE varchar(32) USING gender::text;
    END IF;

    -- instructor_documents.status (document_status_enum), default 'PENDING'
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'instructor_documents'
                 AND column_name = 'status' AND data_type = 'USER-DEFINED') THEN
        ALTER TABLE instructor_documents ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE instructor_documents ALTER COLUMN status TYPE varchar(32) USING status::text;
        ALTER TABLE instructor_documents ALTER COLUMN status SET DEFAULT 'PENDING';
    END IF;

    -- instructor_skills.proficiency_level (proficiency_level_enum), NOT NULL
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'instructor_skills'
                 AND column_name = 'proficiency_level' AND data_type = 'USER-DEFINED') THEN
        ALTER TABLE instructor_skills
            ALTER COLUMN proficiency_level TYPE varchar(32) USING proficiency_level::text;
    END IF;

    -- course_creator_skills.proficiency_level (proficiency_level_enum), NOT NULL
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'course_creator_skills'
                 AND column_name = 'proficiency_level' AND data_type = 'USER-DEFINED') THEN
        ALTER TABLE course_creator_skills
            ALTER COLUMN proficiency_level TYPE varchar(32) USING proficiency_level::text;
    END IF;
END
$$;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_gender;
ALTER TABLE users ADD CONSTRAINT chk_users_gender
    CHECK (gender IN ('MALE', 'FEMALE', 'PREFER_NOT_TO_SAY'));

ALTER TABLE instructor_documents DROP CONSTRAINT IF EXISTS chk_instructor_documents_status;
ALTER TABLE instructor_documents ADD CONSTRAINT chk_instructor_documents_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'));

ALTER TABLE instructor_skills DROP CONSTRAINT IF EXISTS chk_instructor_skills_proficiency_level;
ALTER TABLE instructor_skills ADD CONSTRAINT chk_instructor_skills_proficiency_level
    CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'));

ALTER TABLE course_creator_skills DROP CONSTRAINT IF EXISTS chk_course_creator_skills_proficiency_level;
ALTER TABLE course_creator_skills ADD CONSTRAINT chk_course_creator_skills_proficiency_level
    CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'));

-- Drop the enum types once no column uses them (proficiency_level was never attached to a column).
DO
$$
DECLARE
    enum_type text;
BEGIN
    FOREACH enum_type IN ARRAY ARRAY['gender', 'document_status_enum', 'proficiency_level_enum', 'proficiency_level']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_type t
                   WHERE t.typname = enum_type AND t.typtype = 'e'
                     AND t.typnamespace = current_schema()::regnamespace)
           AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
                           WHERE c.udt_schema = current_schema() AND c.udt_name = enum_type) THEN
            EXECUTE format('DROP TYPE %I', enum_type);
        END IF;
    END LOOP;
END
$$;
