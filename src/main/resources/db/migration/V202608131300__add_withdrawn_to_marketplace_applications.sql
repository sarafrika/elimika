-- Let an instructor withdraw a marketplace job application before they are
-- assigned. WITHDRAWN is terminal for the organisation's review workflow but
-- still re-appliable, so the instructor can apply again while the job is open.
ALTER TABLE class_marketplace_job_applications
    DROP CONSTRAINT IF EXISTS chk_class_marketplace_job_applications_status;

ALTER TABLE class_marketplace_job_applications
    ADD CONSTRAINT chk_class_marketplace_job_applications_status
        CHECK (status IN (
            'PENDING', 'SHORTLISTED', 'INTERVIEWING', 'OFFERED',
            'APPROVED', 'REJECTED', 'ASSIGNED', 'NOT_SELECTED', 'WITHDRAWN'
        ));
