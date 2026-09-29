-- Program publishing used to set only is_active, leaving status at draft and is_published false.
-- Publishing now sets status = 'published' and is_published = true (as course publishing does),
-- and the catalogue, /programs/published and the admin pending queue all key off status.
-- Bring programs that were published under the old flow in line so they keep their visibility.
-- Idempotent: every statement only touches rows that are not yet in the target state.

-- Programs a creator published under the old flow (active, never archived). Approved ones stay
-- live; unapproved ones now appear in the admin pending queue (admin_approved = false, published).
UPDATE training_programs
SET status       = 'published',
    is_published = true
WHERE is_active = true
  AND status IN ('draft', 'in_review');

-- Keep is_published in step with status for every other row.
UPDATE training_programs
SET is_published = true
WHERE status = 'published'
  AND is_published IS DISTINCT FROM true;

UPDATE training_programs
SET is_published = false
WHERE status <> 'published'
  AND is_published = true;
