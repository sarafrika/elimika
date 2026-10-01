# Open classes on the public course page

A signed-out visitor on `/courses/{uuid}` sees the classes they can still join and what those classes cost.
Learners pay the **class fee** (`class_definitions.sale_price`), not the course's own `courses.price` (often 0),
so the page's "Starting from" figure comes from the classes. Revenue terms stay hidden from anonymous course
reads. The class fee is the public sticker price, so it is shown.

## Flow

```
 UI                                   API                                               storage
 ──                                   ───                                               ───────
 Public course page /courses/{uuid}   (no token needed)
   GET /api/v1/courses/{uuid}                  ──► course record, public view (no revenue terms)
   GET /api/v1/courses/{uuid}/open-classes     operationId getCourseOpenClasses, permitAll
        │
        ▼
 CourseOpenClassesController (course) ──► CourseOpenClassServiceImpl (course)
        │ 1 course gate: CourseServiceImpl.isPublicCourse          courses: parent_course_uuid IS NULL,
        │   (the anonymous course read's rule) else 404              status=published, active, admin_approved
        │ 2 ClassDefinitionLookupService.findOpenClassesForCourse  (shared.spi, implemented in classes)
        │      open = is_active AND class_visibility = PUBLIC      class_definitions
        │             AND registration_period_end_date >= today(UTC) or null
        │             AND academic_period_end_date    >= today(UTC) or null
        │      seats: EnrollmentLookupService                     class_enrollments ⨝ scheduled_instances,
        │             .countFilledSeatsByClassDefinition            status NOT IN (CANCELLED, WAITLISTED),
        │             (one grouped query)                           distinct learners per class
        │      branch: TrainingBranchLookupService.findBranchNames training_branches (one query)
        │ 3 place_name = first comma part of location_name; area = the rest minus a trailing country
        │   availability = FULL (0 left) | FEW_LEFT (≤ max(5, 20% of cap)) | OPEN (else, or cap unknown);
        │   the seat numbers themselves stop here and are never serialised
        │ 4 sort: non-FULL first, then fee asc (nulls last), starts_on asc, title
        │   price_from = min(fee) and open_class_count = count, both over non-FULL classes only
        │   currency = commerce.internal.default-currency (the currency classes are charged in)
        ▼
 { price_from, currency_code, open_class_count,
   classes[{ uuid, title, location_type, session_format, place_name, area, fee, currency_code,
             availability, starts_on, ends_on, registration_closes_on, branch_name }] }

 Catalogue page
   GET /api/v1/catalogue/search  (permitAll)
        ──► CatalogueSearchService.hydrate (course)
              courses index hit ──► ClassDefinitionLookupService.summariseOpenClassesByCourse
                                    two queries per page: the open classes of every course on it
                                    (class_definitions), then one grouped enrolment count; FULL
                                    classes dropped, then COUNT + MIN(sale_price) per course
        ──► each course card gains price_from (nullable) and open_class_count; programmes: null and 0.
            No reindex: the figures are live from the database, like lesson and learner counts.
```

## Contract notes

- **404** unless the course is publicly visible (root, published, active, admin-approved). A signed-in author
  previewing a draft gets the same 404; this endpoint is the public face only.
- `price_from` is null when no class can be joined; `currency_code` is null when the list is empty. A class with
  no fee sorts after priced ones and does not set `price_from`.
- **No seat numbers are published.** Seats filled beside seats offered, next to a published fee, turn the page
  into a revenue calculator (the same reason course stats publish fill only as a rounded percentage). Each class
  carries `availability` instead, computed from seats left = cap minus live enrolments (RESERVED, ENROLLED,
  ATTENDED and ABSENT hold a seat; CANCELLED and WAITLISTED do not):
  - `FULL`: no seat left. Still listed, sorted last, and excluded from `price_from` and `open_class_count`
    (here and on catalogue cards).
  - `FEW_LEFT`: seats left ≤ max(5, 20% of the cap).
  - `OPEN`: otherwise, and whenever the cap is unknown.
- `starts_on` is the academic period start, else the first session's day; `ends_on` is the academic period end
  (null when open-ended); `registration_closes_on` is the registration period end.
- Never serialised: coordinates, seat counts or capacity, meeting link, instructor or organisation identifiers,
  instructor pay, revenue split. There is no field for them in `OpenClassSummary`.
- `class_count` on catalogue cards (active public classes, past or not) is unchanged. `open_class_count` counts
  only joinable (open, not FULL) ones.
- No rate limiter fronts anonymous reads today (the only limiter is the user-number lookup). The route is a
  bounded read of one course's classes.

## Ownership

The endpoint lives in the **course** module because the course gate is course data. Class data stays in
**classes** and is reached only through `shared.spi.ClassDefinitionLookupService`, which returns the narrow
`OpenClassListing` / `CourseOpenClassSummary` records.
