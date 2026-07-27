-- Add recruitment-funnel stages (shortlisted / interviewing / offered) to a
-- marketplace job application, between the applicant applying (PENDING) and the
-- final approve/assign/reject decisions.
ALTER TABLE class_marketplace_job_applications
    DROP CONSTRAINT IF EXISTS chk_class_marketplace_job_applications_status;

ALTER TABLE class_marketplace_job_applications
    ADD CONSTRAINT chk_class_marketplace_job_applications_status
        CHECK (status IN (
            'PENDING', 'SHORTLISTED', 'INTERVIEWING', 'OFFERED',
            'APPROVED', 'REJECTED', 'ASSIGNED', 'NOT_SELECTED'
        ));
