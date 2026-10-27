-- Wallet skills verified by programme grading name the programme; null means the learner added it.
ALTER TABLE user_skills
    ADD COLUMN IF NOT EXISTS source_program_uuid UUID REFERENCES training_programs (uuid) ON DELETE SET NULL;
