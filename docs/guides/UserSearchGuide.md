# User Search API Guide

## Overview

The User Search API provides comprehensive search capabilities across user data, including computed fields and related entities. This guide demonstrates how to search users by various criteria including user domains, organization affiliations, and computed fields like full names.

## Search Endpoint

```
GET /api/v1/users/search
```

## Architecture

The implementation uses a two-tier approach:

1. **GenericSpecificationBuilder**: Handles standard entity field searches
2. **UserSpecificationBuilder**: Extends with domain-specific predicates for:
   - User domain filtering
   - Organization affiliation queries
   - Branch assignment searches
   - Computed field searches (full_name)

## Search Parameters

### Standard User Fields

All direct fields from the `User` entity are searchable using the standard operations:

| Field | Type | Operations | Example |
|-------|------|------------|---------|
| `firstName` | String | eq, like, startswith, endswith | `firstName_like=john` |
| `middleName` | String | eq, like, startswith, endswith | `middleName=A` |
| `lastName` | String | eq, like, startswith, endswith | `lastName_startswith=Sm` |
| `email` | String | eq, like, startswith, endswith | `email_endswith=@example.com` |
| `username` | String | eq, like, startswith, endswith | `username=johndoe` |
| `phoneNumber` | String | eq, like, startswith, endswith | `phoneNumber_like=254` |
| `active` | Boolean | eq | `active=true` |
| `gender` | Enum | eq, in | `gender=MALE` |
| `dob` | Date | eq, gt, lt, gte, lte, between | `dob_gt=1990-01-01` |
| `keycloakId` | String | eq | `keycloakId=abc123` |

### Domain-Specific Search Parameters

#### User Domain Search

Search for users with specific domains (roles):

```bash
# Find all students
GET /api/v1/users/search?user_domain=student

# Find all instructors
GET /api/v1/users/search?user_domain=instructor

# Combined with other criteria
GET /api/v1/users/search?user_domain=student&active=true&firstName_like=john
```

**Note**: This searches across both standalone domains and organization-specific domains.

#### Organization Affiliation Search

Search for users belonging to specific organizations:

```bash
# Find all users in an organization (active and inactive)
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001

# Find only active members of an organization
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001&active_in_organisation=true

# Find inactive members
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001&active_in_organisation=false
```

#### Domain Within Organization Search

Search for users with specific roles in a specific organization:

```bash
# Find all students in a specific organization
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001&domain_in_organisation=student

# Find all instructors in a specific organization
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001&domain_in_organisation=instructor
```

**Note**: `domain_in_organisation` requires `organisation_uuid` to be specified.

#### Branch Assignment Search

Search for users assigned to specific training branches:

```bash
# Find all users in a specific branch
GET /api/v1/users/search?branch_uuid=650e8400-e29b-41d4-a716-446655440002
```

### Computed Field Searches

#### Full Name Search

Search by full name (concatenation of firstName + middleName + lastName):

```bash
# Exact match
GET /api/v1/users/search?full_name=John A. Doe

# Partial match (LIKE)
GET /api/v1/users/search?full_name_like=john doe

# Case-insensitive search
GET /api/v1/users/search?full_name_like=JOHN
```

### JPA Relationship Traversal

With the added JPA relationships, you can also search through nested entities:

```bash
# Search by domain name through relationship
GET /api/v1/users/search?domainMappings.userDomain.domainName=student

# Search by organization through relationship
GET /api/v1/users/search?organisationDomainMappings.organisationUuid=550e8400-e29b-41d4-a716-446655440001

# Search by domain in organization mapping
GET /api/v1/users/search?organisationDomainMappings.domain.domainName=instructor

# Search for active organization mappings only
GET /api/v1/users/search?organisationDomainMappings.active=true
```

## Search Operations

### Available Operations

Append these suffixes to field names to specify the operation:

| Operation | Suffix | Description | Example |
|-----------|--------|-------------|---------|
| Equal | `_eq` or none | Exact match | `email=user@example.com` |
| Like | `_like` | Case-insensitive contains | `firstName_like=john` |
| Starts With | `_startswith` | Case-insensitive prefix match | `lastName_startswith=Sm` |
| Ends With | `_endswith` | Case-insensitive suffix match | `email_endswith=@company.com` |
| Greater Than | `_gt` | Numeric/date comparison | `dob_gt=1990-01-01` |
| Less Than | `_lt` | Numeric/date comparison | `dob_lt=2000-12-31` |
| Greater or Equal | `_gte` | Numeric/date comparison | `dob_gte=1990-01-01` |
| Less or Equal | `_lte` | Numeric/date comparison | `dob_lte=2000-12-31` |
| Between | `_between` | Range (comma-separated) | `dob_between=1990-01-01,2000-12-31` |
| In | `_in` | Value in list (comma-separated) | `gender_in=MALE,FEMALE` |
| Not In | `_notin` | Value not in list | `gender_notin=PREFER_NOT_TO_SAY` |
| Not Equal | `_noteq` | Not equal to value | `active_noteq=false` |

## Pagination and Sorting

### Pagination Parameters

| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `page` | Page number (0-indexed) | 0 | `page=2` |
| `size` | Items per page | 20 | `size=50` |
| `sort` | Sort fields and direction | - | `sort=lastName,asc` |

### Examples

```bash
# First page, 10 items per page
GET /api/v1/users/search?firstName_like=john&page=0&size=10

# Sort by last name ascending
GET /api/v1/users/search?active=true&sort=lastName,asc

# Multiple sort fields
GET /api/v1/users/search?active=true&sort=lastName,asc&sort=firstName,asc

# Third page, sorted by creation date descending
GET /api/v1/users/search?user_domain=student&page=2&size=20&sort=createdDate,desc
```

## Complete Examples

### Example 1: Find Active Students Named John

```bash
GET /api/v1/users/search?user_domain=student&firstName_like=john&active=true&page=0&size=20&sort=lastName,asc
```

**Response:**
```json
{
  "status": "success",
  "message": "Users search successful",
  "data": {
    "content": [
      {
        "uuid": "550e8400-e29b-41d4-a716-446655440001",
        "first_name": "John",
        "middle_name": "A",
        "last_name": "Doe",
        "email": "john.doe@example.com",
        "username": "johndoe",
        "active": true,
        "gender": "MALE",
        "dob": "1995-05-15",
        "user_domain": ["student"],
        "organisation_affiliations": [
          {
            "organisation_uuid": "org-123",
            "organisation_name": "University ABC",
            "domain_in_organisation": "student",
            "active": true
          }
        ],
        "full_name": "John A Doe",
        "display_name": "John Doe"
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 1,
    "total_pages": 1,
    "self": "http://localhost:8080/api/v1/users/search?user_domain=student&firstName_like=john&active=true&page=0&size=20&sort=lastName,asc"
  }
}
```

### Example 2: Find All Instructors in a Specific Organization

```bash
GET /api/v1/users/search?organisation_uuid=550e8400-e29b-41d4-a716-446655440001&domain_in_organisation=instructor&active_in_organisation=true
```

### Example 3: Find Users Born in the 1990s

```bash
GET /api/v1/users/search?dob_between=1990-01-01,1999-12-31&sort=dob,asc
```

### Example 4: Find Users by Email Domain

```bash
GET /api/v1/users/search?email_endswith=@company.com&active=true
```

### Example 5: Complex Multi-Criteria Search

```bash
GET /api/v1/users/search?user_domain=student&organisation_uuid=550e8400-e29b-41d4-a716-446655440001&full_name_like=john&dob_gt=1995-01-01&gender=MALE&active=true&page=0&size=10&sort=lastName,asc&sort=firstName,asc
```

## Best Practices

### 1. Use Specific Domain Searches

Instead of fetching all users and filtering client-side:

❌ **Bad**:
```bash
GET /api/v1/users?page=0&size=1000
# Then filter for students in JavaScript
```

✅ **Good**:
```bash
GET /api/v1/users/search?user_domain=student&page=0&size=20
```

### 2. Combine Criteria for Efficiency

Use multiple parameters in a single query:

```bash
GET /api/v1/users/search?user_domain=instructor&organisation_uuid=org-123&active=true
```

### 3. Use Pagination

Always use pagination for potentially large result sets:

```bash
GET /api/v1/users/search?user_domain=student&page=0&size=20
```

### 4. Leverage Full Name Search

For user-friendly name searches:

```bash
GET /api/v1/users/search?full_name_like=john doe
```

Instead of:
```bash
GET /api/v1/users/search?firstName_like=john&lastName_like=doe
```

### 5. Use Appropriate Operations

- Use `_like` for flexible text searches
- Use `_startswith` for prefix matching (better performance than `_like`)
- Use `_eq` (or no suffix) for exact matches
- Use `_between` for date ranges

## Performance Considerations

1. **Indexed Fields**: Searches on `uuid`, `email`, and `username` are fastest due to database indexes
2. **Relationship Queries**: Domain and organization searches use subqueries, which may be slower on large datasets
3. **Full Name Searches**: Computed at query time, slightly slower than direct field searches
4. **Pagination**: Always use appropriate page sizes (10-50 recommended)
5. **Sorting**: Limit sort fields to 2-3 for best performance

## Troubleshooting

### No Results Returned

1. Check that parameter names are correct (case-sensitive)
2. Verify domain names are lowercase (`student`, not `Student`)
3. Ensure UUIDs are valid format
4. Check that `organisation_uuid` is provided when using `domain_in_organisation`

### Slow Queries

1. Reduce page size
2. Limit the number of search criteria
3. Use indexed fields when possible
4. Consider using specific endpoints for organization-based queries

### Invalid Parameter Errors

1. Verify operation suffixes are correct (`_like`, not `_contains`)
2. Check date formats are ISO 8601 (`YYYY-MM-DD`)
3. Ensure UUIDs are properly formatted
4. Validate enum values (`MALE`, `FEMALE`, etc.)

## API Specifications

For complete API specifications including request/response schemas, see the Swagger documentation at:

```
http://localhost:8080/swagger-ui.html
```

Navigate to "Users API" → "Search users" for interactive API testing.