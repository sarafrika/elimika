-- Records when an organisation submitted itself for admin verification. Lets the
-- organisation dashboard show a real "submitted / awaiting review" state instead of
-- only the terminal admin_verified flag, and gives admins a request queue to work.
-- Nullable — existing rows keep NULL (never submitted).
ALTER TABLE organisation ADD COLUMN IF NOT EXISTS verification_requested_at TIMESTAMP;
