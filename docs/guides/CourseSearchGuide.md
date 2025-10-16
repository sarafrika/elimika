# Course Search API Guide

## Overview

This guide provides comprehensive documentation for the Course Search API, which enables powerful and flexible searching across all course attributes including computed fields, relationships, and complex filters.

The search functionality combines a custom `CourseSpecificationBuilder` with the generic specification builder to support both domain-specific course searches and standard field filtering.

## Table of Contents

1. [Basic Usage](#basic-usage)
2. [Search Parameters](#search-parameters)
3. [Standard Field Searches](#standard-field-searches)
4. [Relationship-Based Searches](#relationship-based-searches)
5. [Computed Field Searches](#computed-field-searches)
6. [Status-Based Searches](#status-based-searches)
7. [Price-Based Searches](#price-based-searches)
8. [Enrollment-Based Searches](#enrollment-based-searches)
9. [Advanced Search Examples](#advanced-search-examples)
10. [Operation Modifiers](#operation-modifiers)
11. [Pagination and Sorting](#pagination-and-sorting)
12. [Technical Implementation](#technical-implementation)

---

## Basic Usage

### Endpoint

```
GET /api/v1/courses/search
```

### Request Format

All search parameters are passed as query parameters:

```http
GET /api/v1/courses/search?name_like=python&is_published=true&page=0&size=20
```

### Response Format

```json
{
  "content": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Python Programming Basics",
      "description": "Learn Python from scratch",
      "course_creator_uuid": "660e8400-e29b-41d4-a716-446655440001",
      "difficulty_uuid": "770e8400-e29b-41d4-a716-446655440002",
      "price": 49.99,
      "duration_hours": 10,
      "duration_minutes": 30,
      "status": "PUBLISHED",
      "active": true,
      "is_free": false,
      "is_published": true,
      "lifecycle_stage": "published",
      "category_names": ["Programming", "Python"],
      "category_uuids": ["880e8400-e29b-41d4-a716-446655440003"],
      "category_count": 2,
      "total_duration_display": "10h 30m",
      "accepts_new_enrollments": true,
      "has_multiple_categories": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Search Parameters

### Core Course Fields

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `name` | String | Exact course name | `name=Python Basics` |
| `name_like` | String | Partial name match (case-insensitive) | `name_like=python` |
| `description_like` | String | Partial description match | `description_like=beginner` |
| `objectives_like` | String | Search in course objectives | `objectives_like=loops` |
| `prerequisites_like` | String | Search in prerequisites | `prerequisites_like=basic` |
| `course_creator_uuid` | UUID | Filter by course creator | `course_creator_uuid=550e8400-...` |
| `difficulty_uuid` | UUID | Filter by difficulty level UUID | `difficulty_uuid=770e8400-...` |
| `active` | Boolean | Filter by active status | `active=true` |

### Duration Fields

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `duration_hours` | Integer | Exact duration in hours | `duration_hours=10` |
| `duration_hours_gte` | Integer | Minimum hours | `duration_hours_gte=5` |
| `duration_hours_lte` | Integer | Maximum hours | `duration_hours_lte=20` |
| `duration_minutes` | Integer | Exact duration in minutes | `duration_minutes=30` |

### Class and Age Limits

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `class_limit` | Integer | Exact class size limit | `class_limit=30` |
| `class_limit_gte` | Integer | Minimum class limit | `class_limit_gte=20` |
| `age_lower_limit` | Integer | Exact lower age limit | `age_lower_limit=18` |
| `age_upper_limit` | Integer | Exact upper age limit | `age_upper_limit=65` |

### Media URLs

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `thumbnail_url_like` | String | Search thumbnail URLs | `thumbnail_url_like=image` |
| `intro_video_url_like` | String | Search intro video URLs | `intro_video_url_like=intro` |
| `banner_url_like` | String | Search banner URLs | `banner_url_like=banner` |

---

## Standard Field Searches

Standard fields support various operation modifiers through underscore suffixes.

### Exact Match

```http
GET /api/v1/courses/search?name=Python Programming Basics
```

### Partial Match (LIKE)

```http
GET /api/v1/courses/search?name_like=python
```

Searches for courses with names containing "python" (case-insensitive).

### Greater Than / Less Than

```http
GET /api/v1/courses/search?duration_hours_gte=5&duration_hours_lte=15
```

Finds courses between 5 and 15 hours long.

### Range (BETWEEN)

```http
GET /api/v1/courses/search?price_between=0,100
```

Finds courses priced between $0 and $100.

### IN / NOT IN

```http
GET /api/v1/courses/search?status_in=PUBLISHED,IN_REVIEW
```

Finds courses with PUBLISHED or IN_REVIEW status.

---

## Relationship-Based Searches

### Search by Category Name

Search across the `course_category_mappings` junction table to find courses by category name:

```http
GET /api/v1/courses/search?category_name=programming
```

**How it works:**
- Uses a subquery to check the junction table
- Performs case-insensitive partial matching on category names
- Returns courses associated with any matching category

**Example:**
```http
GET /api/v1/courses/search?category_name=web&is_published=true
```
Finds all published courses in web-related categories.

### Search by Difficulty Name

Search by difficulty level name instead of UUID:

```http
GET /api/v1/courses/search?difficulty_name=beginner
```

**How it works:**
- Joins with the `difficulty_levels` table
- Performs case-insensitive partial matching on difficulty names
- Useful when you don't have the difficulty UUID

**Example:**
```http
GET /api/v1/courses/search?difficulty_name=intermediate&category_name=programming
```

---

## Computed Field Searches

These parameters filter on fields computed in the CourseDTO that don't exist directly in the database.

### Lifecycle Stage

Search by computed lifecycle stage (mapped from ContentStatus):

```http
GET /api/v1/courses/search?lifecycle_stage=published
```

**Valid values:**
- `draft` - Maps to ContentStatus.DRAFT
- `in_review` - Maps to ContentStatus.IN_REVIEW
- `published` - Maps to ContentStatus.PUBLISHED
- `archived` - Maps to ContentStatus.ARCHIVED

**Example:**
```http
GET /api/v1/courses/search?lifecycle_stage=draft&course_creator_uuid=550e8400-...
```
Finds all draft courses by a specific creator.

### Free Courses (is_free)

Filter courses that are free (price is null or zero):

```http
GET /api/v1/courses/search?is_free=true
```

**How it works:**
- `is_free=true` - Returns courses where price is NULL or 0
- `is_free=false` - Returns courses with price > 0

**Example:**
```http
GET /api/v1/courses/search?is_free=true&category_name=programming
```
Finds all free programming courses.

### Accepts New Enrollments

Filter courses accepting new student enrollments:

```http
GET /api/v1/courses/search?accepts_new_enrollments=true
```

**How it works:**
A course accepts new enrollments if ALL conditions are met:
1. Status is PUBLISHED
2. Active flag is true
3. Either `class_limit` is null OR current enrollment count < `class_limit`

**Example:**
```http
GET /api/v1/courses/search?accepts_new_enrollments=true&difficulty_name=beginner
```
Finds beginner courses currently accepting enrollments.

---

## Status-Based Searches

Quick filters for specific ContentStatus values:

### Published Courses

```http
GET /api/v1/courses/search?is_published=true
```

Equivalent to `status=PUBLISHED`.

### Draft Courses

```http
GET /api/v1/courses/search?is_draft=true
```

Equivalent to `status=DRAFT`.

### Archived Courses

```http
GET /api/v1/courses/search?is_archived=true
```

Equivalent to `status=ARCHIVED`.

### In Review Courses

```http
GET /api/v1/courses/search?is_in_review=true
```

Equivalent to `status=IN_REVIEW`.

### Combining Status Filters

You can use the direct status field with modifiers:

```http
GET /api/v1/courses/search?status_in=PUBLISHED,IN_REVIEW
```

---

## Price-Based Searches

### Exact Price

```http
GET /api/v1/courses/search?price=49.99
```

### Price Range

```http
GET /api/v1/courses/search?min_price=20&max_price=100
```

Finds courses priced between $20 and $100.

### Greater Than Price

```http
GET /api/v1/courses/search?price_gte=50
```

### Less Than Price

```http
GET /api/v1/courses/search?price_lte=30
```

### Free or Paid

```http
# Free courses
GET /api/v1/courses/search?is_free=true

# Paid courses only
GET /api/v1/courses/search?is_free=false
```

---

## Enrollment-Based Searches

### Courses With Enrollments

Find courses that have at least one enrollment:

```http
GET /api/v1/courses/search?has_enrollments=true
```

**How it works:**
- Uses a subquery to count enrollments
- `has_enrollments=true` - Courses with enrollment count > 0
- `has_enrollments=false` - Courses with no enrollments

### Courses Without Enrollments

```http
GET /api/v1/courses/search?has_enrollments=false&is_published=true
```

Finds published courses that haven't enrolled any students yet.

### Combining with Other Filters

```http
GET /api/v1/courses/search?has_enrollments=true&category_name=data+science&min_price=50
```

Finds data science courses over $50 that have active enrollments.

---

## Advanced Search Examples

### Example 1: Find Popular Beginner Courses

```http
GET /api/v1/courses/search?difficulty_name=beginner&has_enrollments=true&is_published=true&price_lte=50
```

Returns beginner courses that:
- Are published
- Have enrollments (popular)
- Cost $50 or less

### Example 2: Find Available Free Programming Courses

```http
GET /api/v1/courses/search?category_name=programming&is_free=true&accepts_new_enrollments=true
```

Returns courses that:
- Are in programming categories
- Are completely free
- Are currently accepting new students

### Example 3: Find Long-Form Advanced Courses

```http
GET /api/v1/courses/search?difficulty_name=advanced&duration_hours_gte=20&is_published=true
```

Returns advanced courses that are 20+ hours long and published.

### Example 4: Find Courses by Creator Status

```http
GET /api/v1/courses/search?course_creator_uuid=550e8400-...&lifecycle_stage=draft
```

Returns all draft courses by a specific creator.

### Example 5: Find Courses Ready for Review

```http
GET /api/v1/courses/search?is_draft=true&description_like=%&objectives_like=%
```

Returns draft courses that have both description and objectives filled in.

### Example 6: Age-Appropriate Course Search

```http
GET /api/v1/courses/search?age_lower_limit_lte=12&age_upper_limit_gte=12&is_published=true
```

Finds courses suitable for 12-year-olds.

### Example 7: Multi-Category Filter

```http
GET /api/v1/courses/search?category_name=web&difficulty_name=intermediate&min_price=30&max_price=100
```

Finds intermediate web development courses in the $30-$100 price range.

### Example 8: Content Moderation Query

```http
GET /api/v1/courses/search?is_in_review=true&sort=createdAt,asc
```

Returns courses pending review, sorted by submission date.

---

## Operation Modifiers

Operation modifiers are appended to field names with an underscore to change the comparison operation.

### Available Modifiers

| Modifier | Description | Example | SQL Equivalent |
|----------|-------------|---------|----------------|
| (none) | Exact match | `name=Python` | `name = 'Python'` |
| `_like` | Contains (case-insensitive) | `name_like=python` | `LOWER(name) LIKE '%python%'` |
| `_startswith` | Starts with | `name_startswith=Python` | `LOWER(name) LIKE 'python%'` |
| `_endswith` | Ends with | `name_endswith=Basics` | `LOWER(name) LIKE '%basics'` |
| `_gt` | Greater than | `price_gt=50` | `price > 50` |
| `_gte` | Greater than or equal | `price_gte=50` | `price >= 50` |
| `_lt` | Less than | `price_lt=100` | `price < 100` |
| `_lte` | Less than or equal | `price_lte=100` | `price <= 100` |
| `_between` | Range (inclusive) | `price_between=50,100` | `price BETWEEN 50 AND 100` |
| `_in` | Match any value | `status_in=PUBLISHED,ARCHIVED` | `status IN ('PUBLISHED', 'ARCHIVED')` |
| `_notin` | Exclude values | `status_notin=DRAFT,ARCHIVED` | `status NOT IN ('DRAFT', 'ARCHIVED')` |
| `_noteq` | Not equal | `status_noteq=ARCHIVED` | `status != 'ARCHIVED'` |

### String Operations

```http
# Contains "python" anywhere in name
GET /api/v1/courses/search?name_like=python

# Starts with "Introduction"
GET /api/v1/courses/search?name_startswith=Introduction

# Ends with "Fundamentals"
GET /api/v1/courses/search?name_endswith=Fundamentals
```

### Numeric Operations

```http
# Greater than
GET /api/v1/courses/search?price_gt=50

# Greater than or equal
GET /api/v1/courses/search?duration_hours_gte=10

# Less than
GET /api/v1/courses/search?class_limit_lt=30

# Less than or equal
GET /api/v1/courses/search?age_lower_limit_lte=18
```

### Range Operations

```http
# Price between $20 and $100
GET /api/v1/courses/search?price_between=20,100

# Duration between 5 and 15 hours
GET /api/v1/courses/search?duration_hours_between=5,15
```

### List Operations

```http
# Match any of these statuses
GET /api/v1/courses/search?status_in=PUBLISHED,IN_REVIEW

# Exclude these statuses
GET /api/v1/courses/search?status_notin=DRAFT,ARCHIVED

# Not equal to specific value
GET /api/v1/courses/search?status_noteq=ARCHIVED
```

---

## Pagination and Sorting

### Pagination Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Page number (0-indexed) |
| `size` | Integer | 20 | Number of results per page |

### Sorting Parameters

Use the `sort` parameter with format: `field,direction`

**Direction values:**
- `asc` - Ascending order
- `desc` - Descending order

### Examples

```http
# First page with 10 results
GET /api/v1/courses/search?is_published=true&page=0&size=10

# Sort by name ascending
GET /api/v1/courses/search?sort=name,asc

# Sort by price descending
GET /api/v1/courses/search?is_published=true&sort=price,desc

# Sort by creation date (newest first)
GET /api/v1/courses/search?sort=createdAt,desc

# Multiple sort fields
GET /api/v1/courses/search?sort=status,asc&sort=price,desc

# Sort by name with pagination
GET /api/v1/courses/search?category_name=programming&page=2&size=15&sort=name,asc
```

### Common Sort Fields

- `name` - Course name
- `price` - Course price
- `createdAt` - Creation timestamp
- `updatedAt` - Last update timestamp
- `durationHours` - Duration in hours
- `status` - Content status
- `classLimit` - Maximum class size

---

## Technical Implementation

### Architecture

The course search functionality uses a two-layer approach:

1. **CourseSpecificationBuilder** - Handles domain-specific course searches
   - Computed field searches (is_free, lifecycle_stage, accepts_new_enrollments)
   - Relationship traversal (category_name, difficulty_name)
   - Complex enrollment-based queries
   - Custom predicates for course-specific logic

2. **GenericSpecificationBuilder** - Handles standard field searches
   - Direct field matching with operation modifiers
   - Works on any field in the Course entity
   - Provides consistent behavior across all entities

### Custom Parameters Handled by CourseSpecificationBuilder

These parameters are extracted and processed specially:

- `category_name` - Uses subquery on junction table
- `difficulty_name` - Joins difficulty_levels table
- `lifecycle_stage` - Maps to ContentStatus enum
- `is_free` - Checks price is null or zero
- `is_published`, `is_draft`, `is_archived`, `is_in_review` - Status shortcuts
- `min_price`, `max_price` - Price range filters
- `has_enrollments` - Subquery on enrollments
- `accepts_new_enrollments` - Complex multi-condition check
- `course_creator_uuid` - Direct UUID match
- `active` - Boolean active flag

All other parameters are passed to GenericSpecificationBuilder for standard processing.

### Database Relationships

The implementation leverages JPA relationships added to the Course entity:

```java
// Read-only relationship for querying
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "difficulty_uuid", referencedColumnName = "uuid",
        insertable = false, updatable = false)
private DifficultyLevel difficulty;

// Read-only relationship for enrollment queries
@OneToMany(fetch = FetchType.LAZY)
@JoinColumn(name = "course_uuid", referencedColumnName = "uuid",
        insertable = false, updatable = false)
private List<CourseEnrollment> enrollments;
```

These relationships are:
- Read-only (insertable=false, updatable=false)
- Lazily fetched (FetchType.LAZY)
- Used only for query construction, not data modification

### Subquery Examples

#### Category Name Search

```java
Subquery<UUID> subquery = query.subquery(UUID.class);
Root<CourseCategoryMapping> mappingRoot = subquery.from(CourseCategoryMapping.class);
Join<CourseCategoryMapping, Category> categoryJoin = mappingRoot.join("category");

subquery.select(mappingRoot.get("courseUuid"))
        .where(criteriaBuilder.like(
                criteriaBuilder.lower(categoryJoin.get("name")),
                "%" + categoryName.toLowerCase() + "%"
        ));

return criteriaBuilder.in(root.get("uuid")).value(subquery);
```

#### Enrollment Count Check

```java
Subquery<Long> subquery = query.subquery(Long.class);
Root<CourseEnrollment> enrollmentRoot = subquery.from(CourseEnrollment.class);

subquery.select(criteriaBuilder.count(enrollmentRoot.get("uuid")))
        .where(criteriaBuilder.equal(
                enrollmentRoot.get("courseUuid"),
                root.get("uuid")
        ));

return criteriaBuilder.greaterThan(subquery, 0L);
```

### Performance Considerations

1. **Lazy Loading** - Relationships use FetchType.LAZY to avoid N+1 queries
2. **Subqueries** - Complex filters use subqueries instead of joins for better performance
3. **Index Usage** - Searches leverage database indexes on commonly queried fields
4. **Pagination** - Always use pagination for large result sets
5. **Specific Fields** - Query only needed fields rather than fetching everything

### Adding Custom Search Parameters

To add a new custom search parameter:

1. **Extract the parameter** in `buildCourseSpecification()`:
```java
String myParam = searchParams.get("my_param");
```

2. **Add handling logic**:
```java
if (myParam != null && !myParam.trim().isEmpty()) {
    specifications.add(myCustomSpecification(myParam.trim()));
}
```

3. **Remove from remainingParams**:
```java
remainingParams.remove("my_param");
```

4. **Create the specification method**:
```java
public Specification<Course> myCustomSpecification(String value) {
    return (root, query, criteriaBuilder) -> {
        // Build and return predicate
    };
}
```

5. **Document** the new parameter in this guide

---

## Error Handling

### Invalid Parameter Values

Invalid values are handled gracefully:

```java
try {
    UUID creatorUuid = UUID.fromString(courseCreatorUuid.trim());
    specifications.add(hasCourseCreator(creatorUuid));
} catch (IllegalArgumentException e) {
    log.warn("Invalid course_creator_uuid value: {}", courseCreatorUuid);
}
```

Invalid parameters are logged and ignored rather than causing the entire search to fail.

### Unknown Lifecycle Stage

```http
GET /api/v1/courses/search?lifecycle_stage=invalid
```

Returns no results and logs a warning. Valid stages: draft, in_review, published, archived.

### Malformed Numeric Values

```http
GET /api/v1/courses/search?min_price=abc
```

Logs a warning and ignores the invalid parameter.

---

## Best Practices

### 1. Always Use Pagination

```http
# Good
GET /api/v1/courses/search?is_published=true&page=0&size=20

# Avoid (can return thousands of results)
GET /api/v1/courses/search?is_published=true
```

### 2. Combine Filters for Specific Results

```http
# Good - Specific query
GET /api/v1/courses/search?category_name=python&difficulty_name=beginner&is_free=true

# Less efficient - Too broad
GET /api/v1/courses/search?category_name=programming
```

### 3. Use Computed Fields When Appropriate

```http
# Good - Uses computed field
GET /api/v1/courses/search?is_free=true

# Works but less clear
GET /api/v1/courses/search?price=0
```

### 4. Leverage Status Shortcuts

```http
# Good - Clear intent
GET /api/v1/courses/search?is_published=true&accepts_new_enrollments=true

# Works but verbose
GET /api/v1/courses/search?status=PUBLISHED&active=true
```

### 5. Use Appropriate Operation Modifiers

```http
# Good - Case-insensitive partial match
GET /api/v1/courses/search?name_like=python

# Less flexible - Requires exact match
GET /api/v1/courses/search?name=Python Programming Basics
```

### 6. Consider Query Performance

```http
# Good - Indexed fields
GET /api/v1/courses/search?course_creator_uuid=...&status=PUBLISHED

# Slower - Text search on large field
GET /api/v1/courses/search?description_like=comprehensive
```

---

## Migration from Generic Search

If you're currently using generic search parameters, the new system is backward compatible:

### Old Approach (Still Works)

```http
GET /api/v1/courses/search?status=PUBLISHED&active=true
```

### New Approach (Recommended)

```http
GET /api/v1/courses/search?is_published=true
```

### Migration Benefits

- **Clearer intent** - `is_published=true` vs `status=PUBLISHED`
- **Computed fields** - Access DTO fields like `is_free`, `accepts_new_enrollments`
- **Relationship traversal** - Search by `category_name` instead of category UUID
- **Complex filters** - Use `accepts_new_enrollments` instead of multiple conditions

---

## Support and Feedback

For questions, issues, or feature requests related to course search functionality:

1. Check this documentation first
2. Review the CourseSpecificationBuilder source code
3. Check application logs for warnings about invalid parameters
4. Refer to the GenericSpecificationBuilder documentation for standard operations

---

## Appendix: Complete Parameter Reference

### Custom Parameters (CourseSpecificationBuilder)

| Parameter | Type | Description |
|-----------|------|-------------|
| `category_name` | String | Search by category name (partial match) |
| `difficulty_name` | String | Search by difficulty level name (partial match) |
| `lifecycle_stage` | Enum | draft, in_review, published, archived |
| `is_free` | Boolean | Filter free courses |
| `is_published` | Boolean | Filter published courses (status=PUBLISHED) |
| `is_draft` | Boolean | Filter draft courses (status=DRAFT) |
| `is_archived` | Boolean | Filter archived courses (status=ARCHIVED) |
| `is_in_review` | Boolean | Filter in-review courses (status=IN_REVIEW) |
| `min_price` | Decimal | Minimum price filter |
| `max_price` | Decimal | Maximum price filter |
| `has_enrollments` | Boolean | Filter courses with/without enrollments |
| `accepts_new_enrollments` | Boolean | Filter courses accepting new students |
| `course_creator_uuid` | UUID | Filter by course creator |
| `active` | Boolean | Filter by active status |

### Standard Parameters (GenericSpecificationBuilder)

All Course entity fields support standard operations with modifiers:

- `name`, `name_like`, `name_startswith`, `name_endswith`
- `description`, `description_like`
- `objectives`, `objectives_like`
- `prerequisites`, `prerequisites_like`
- `difficulty_uuid`, `difficulty_uuid_in`
- `price`, `price_gt`, `price_gte`, `price_lt`, `price_lte`, `price_between`
- `duration_hours`, `duration_hours_gte`, `duration_hours_lte`
- `duration_minutes`, `duration_minutes_gte`, `duration_minutes_lte`
- `class_limit`, `class_limit_gte`, `class_limit_lte`
- `age_lower_limit`, `age_lower_limit_lte`
- `age_upper_limit`, `age_upper_limit_gte`
- `thumbnail_url`, `thumbnail_url_like`
- `intro_video_url`, `intro_video_url_like`
- `banner_url`, `banner_url_like`
- `status`, `status_in`, `status_notin`, `status_noteq`

---

**Document Version:** 1.0
**Last Updated:** 2025-10-16
**Implementation:** CourseSpecificationBuilder + GenericSpecificationBuilder