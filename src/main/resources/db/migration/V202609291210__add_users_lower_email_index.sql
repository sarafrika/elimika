-- Email lookups are case-insensitive (lower(email) = lower(:email)); back them with an expression index.
-- Deliberately not UNIQUE: the existing case-sensitive UNIQUE constraint on users.email allows rows
-- that differ only by case, and such rows must be reconciled before uniqueness can be enforced here.
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (lower(email));
