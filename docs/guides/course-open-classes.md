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
        │ 4 sort: fee asc (nulls last), starts_on asc, title;  price_from = min(fee)
        │   currency = commerce.internal.default-currency (the currency classes are charged in)
        ▼
 { price_from, currency_code, open_class_count,
   classes[{ uuid, title, location_type, session_format, place_name, area, fee, currency_code,
             max_participants, seats_left, starts_on, ends_on, registration_closes_on, branch_name }] }

 Catalogue page
   GET /api/v1/catalogue/search  (permitAll)
        ──► CatalogueSearchService.hydrate (course)
              courses index hit ──► ClassDefinitionLookupService.summariseOpenClassesByCourse
                                    one grouped query per page: COUNT + MIN(sale_price) over the same
                                    open-class rule ──► class_definitions
        ──► each course card gains price_from (nullable) and open_class_count; programmes: null and 0.
            No reindex: the figures are live from the database, like lesson and learner counts.
```

## Contract notes

- **404** unless the course is publicly visible (root, published, active, admin-approved). A signed-in author
  previewing a draft gets the same 404; this endpoint is the public face only.
- `price_from` and `currency_code` are null when no class is open. A class with no fee sorts last and does not
  set `price_from`.
- `seats_left` = `max_participants` minus live enrolments, floored at 0; null when the class sets no cap.
  RESERVED (awaiting payment), ENROLLED, ATTENDED and ABSENT all hold a seat.
- `starts_on` is the academic period start, else the first session's day; `ends_on` is the academic period end
  (null when open-ended); `registration_closes_on` is the registration period end.
- Never serialised: coordinates, meeting link, instructor or organisation identifiers, instructor pay, revenue
  split. There is no field for them in `OpenClassSummary`.
- `class_count` on catalogue cards (active public classes, past or not) is unchanged. `open_class_count` counts
  only joinable ones.
- No rate limiter fronts anonymous reads today (the only limiter is the user-number lookup). The route is a
  bounded read of one course's classes.

## Ownership

The endpoint lives in the **course** module because the course gate is course data. Class data stays in
**classes** and is reached only through `shared.spi.ClassDefinitionLookupService`, which returns the narrow
`OpenClassListing` / `CourseOpenClassSummary` records.
