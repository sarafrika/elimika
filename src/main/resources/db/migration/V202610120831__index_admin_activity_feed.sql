-- The admin activity feed reads only /api/v1/admin requests, newest first, and counts them for paging.
-- A partial index keeps both off the full request_audit_log scan; schema only, no rows are touched.
CREATE INDEX IF NOT EXISTS idx_request_audit_admin_activity
    ON request_audit_log (created_date DESC) INCLUDE (response_status)
    WHERE lower(request_uri) LIKE '/api/v1/admin%';
