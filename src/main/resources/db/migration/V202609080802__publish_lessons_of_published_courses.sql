-- Publish the lessons of courses that are already live.
--
-- A lesson's status is stamped from its course's status at the moment the lesson is written, and
-- publishing a course did not restamp them. The ordinary build order -- draft the course, add its
-- lessons, publish it -- therefore left every lesson at 'draft' on a course that is live in the
-- catalogue.
--
-- That is not cosmetic. GET /api/v1/courses/{uuid}/content withholds draft lessons from every
-- footing that is not building the course, so a published course answered prospects *and its own
-- enrolled students* with an empty curriculum and total_lessons: 0 while plainly having lessons.
-- CourseServiceImpl#publishDraftedLessons closes the hole for future publishes; this backfills the
-- courses that were published before it existed.
--
-- Only 'draft' rows on a live course are moved: an archived or in_review lesson keeps its status,
-- so genuinely withheld material stays withheld. `active` is set alongside `status` because
-- check_active_only_if_published permits active = true only on a published row, and because the
-- content endpoint requires both.
UPDATE lessons l
SET status = 'published',
    active = true
FROM courses c
WHERE l.course_uuid = c.uuid
  AND lower(c.status) = 'published'
  AND c.active = true
  AND lower(l.status) = 'draft';
