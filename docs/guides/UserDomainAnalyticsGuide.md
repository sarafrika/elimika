# Elimika User Domain Analytics Guide

## Overview

This guide explains the analytics architecture for all user domains in the Elimika platform. Analytics are decoupled using Service Provider Interfaces (SPIs) to maintain module independence while providing comprehensive insights across domains.

---

## Analytics Architecture

### Core Principles

1. **Decoupled Design**: Each module exposes analytics through SPI interfaces in `apps.sarafrika.elimika.shared.spi.analytics`
2. **Snapshot Pattern**: Analytics are captured as immutable snapshots at query time
3. **Module Independence**: Modules can evolve analytics independently
4. **Aggregation**: Admin dashboard aggregates analytics from all domains

### Analytics SPI Location

All analytics SPIs are located in:
```
src/main/java/apps/sarafrika/elimika/shared/spi/analytics/
```

Implementations are in each module's `service/impl/` package.

---

## 1. Student Domain Analytics

### Purpose
Provide insights into student engagement, progress, and performance for instructors, course creators, and administrators.

###  Available Analytics

Currently, student analytics are integrated within **Course Analytics** and **Timetabling Analytics**.

### Key Metrics

| Metric | Source Module | Description |
|--------|---------------|-------------|
| **Total Enrollments** | Course | All course enrollments across platform |
| **Active Enrollments** | Course | Students currently progressing through courses |
| **Age-Gated Rejections** | Course | Enrollment attempts blocked by age verification (AgeRestrictionException) |
| **Completion Rate** | Course | Percentage of enrollments completed |
| **Average Progress** | Course | Mean progress across all active enrollments |
| **Attendance Rate** | Timetabling | Percentage of attended vs. scheduled sessions |

### Future Enhancements

Consider creating `StudentAnalyticsService` SPI for:
- Student retention metrics
- Learning path completion rates
- Assessment performance trends
- Engagement scores
- Time-to-completion analytics

---

## 2. Instructor Domain Analytics

### Purpose
Track instructor effectiveness, capacity, and compliance status.

### Analytics Service

**Interface**: `InstructorAnalyticsService`
**Implementation**: `InstructorAnalyticsServiceImpl` (instructor/service/impl/)
**Snapshot**: `InstructorAnalyticsSnapshot`

### Key Metrics

```java
public record InstructorAnalyticsSnapshot(
    long verifiedInstructors,           // Total verified instructors
    long pendingInstructors,            // Awaiting admin verification
    long documentsPendingVerification,  // Documents needing review
    long documentsExpiring30d           // Documents expiring soon
) {}
```

### Use Cases

#### For Administrators
- Monitor instructor verification queue
- Track compliance and documentation status
- Identify instructors needing re-verification

#### For Instructor Managers
- Capacity planning based on verified instructor count
- Onboarding pipeline tracking
- Compliance monitoring

### API Endpoint

Admin dashboard: `GET /api/v1/admin/dashboard/statistics`

Returns instructor analytics nested in response:
```json
{
  "instructor_analytics": {
    "verified_instructors": 45,
    "pending_instructors": 5,
    "documents_pending_verification": 12,
    "documents_expiring_30d": 8
  }
}
```

---

## 3. Course Creator Domain Analytics

### Purpose
Monitor course creator verification status and content authorship activity.

### Analytics Service

**Interface**: `CourseCreatorAnalyticsService`
**Implementation**: `CourseCreatorAnalyticsServiceImpl` (coursecreator/service/impl/)
**Snapshot**: `CourseCreatorAnalyticsSnapshot`

### Key Metrics

```java
public record CourseCreatorAnalyticsSnapshot(
    long totalCourseCreators,      // All registered course creators
    long verifiedCourseCreators,   // Admin-verified creators
    long pendingCourseCreators     // Awaiting verification
) {}
```

### Use Cases

#### For Administrators
- Track course creator pipeline
- Monitor verification queue
- Content authorship capacity planning

#### For Content Managers
- Author onboarding status
- Content creator community growth

### API Endpoint

Admin dashboard: `GET /api/v1/admin/dashboard/statistics`

Returns course creator analytics:
```json
{
  "course_creator_analytics": {
    "total_course_creators": 78,
    "verified_course_creators": 65,
    "pending_course_creators": 13
  }
}
```

---

## 4. Course Domain Analytics

### Purpose
Comprehensive learning content and enrollment analytics.

### Analytics Service

**Interface**: `CourseAnalyticsService`
**Implementation**: `CourseAnalyticsServiceImpl` (course/service/impl/)
**Snapshot**: `CourseAnalyticsSnapshot`

### Key Metrics

```java
public record CourseAnalyticsSnapshot(
    // Course Metrics
    long totalCourses,
    long publishedCourses,
    long inReviewCourses,
    long draftCourses,
    long archivedCourses,

    // Enrollment Metrics
    long totalCourseEnrollments,
    long activeCourseEnrollments,
    long newCourseEnrollments7d,
    long completedCourseEnrollments30d,
    double averageCourseProgress,

    // Program Metrics
    long totalTrainingPrograms,
    long publishedTrainingPrograms,
    long activeTrainingPrograms,
    long programEnrollments,
    long completedProgramEnrollments30d
) {}
```

### Use Cases

- Content library health monitoring
- Enrollment trends and forecasting
- Course completion tracking
- Program effectiveness measurement

---

## 5. Timetabling Domain Analytics

### Purpose
Track scheduling efficiency, session attendance, and capacity utilization.

### Analytics Service

**Interface**: `TimetablingAnalyticsService`
**Implementation**: `TimetablingAnalyticsServiceImpl` (timetabling/service/impl/)
**Snapshot**: `TimetablingAnalyticsSnapshot`

### Key Metrics

```java
public record TimetablingAnalyticsSnapshot(
    long sessionsNext7Days,              // Upcoming scheduled sessions
    long sessionsLast30Days,             // Recent sessions
    long sessionsCompletedLast30Days,    // Successfully completed
    long sessionsCancelledLast30Days,    // Cancelled sessions
    long attendedEnrollmentsLast30Days,  // Student attendance
    long absentEnrollmentsLast30Days     // Student absences
) {}
```

### Use Cases

- Session scheduling optimization
- Attendance rate monitoring
- Capacity planning
- Cancellation trend analysis

---

## 6. Commerce Domain Analytics

### Purpose
Track revenue, customer behavior, and purchase patterns.

### Analytics Service

**Interface**: `CommerceAnalyticsService`
**Implementation**: `CommerceAnalyticsServiceImpl` (commerce/purchase/service/impl/)
**Snapshot**: `CommerceAnalyticsSnapshot`

### Key Metrics

```java
public record CommerceAnalyticsSnapshot(
    long totalOrders,
    long ordersLast30Days,
    long capturedOrders,              // Successfully paid orders
    long uniqueCustomers,
    long newCustomersLast30Days,
    long coursePurchasesLast30Days,
    long classPurchasesLast30Days
) {}
```

### Use Cases

- Revenue tracking
- Customer acquisition monitoring
- Purchase pattern analysis
- Product performance comparison (courses vs. classes)

---

## 7. Notifications Domain Analytics

### Purpose
Monitor notification delivery performance and system health.

### Analytics Service

**Interface**: `NotificationAnalyticsService`
**Implementation**: `NotificationAnalyticsServiceImpl` (notifications/service/impl/)
**Snapshot**: `NotificationAnalyticsSnapshot`

### Key Metrics

```java
public record NotificationAnalyticsSnapshot(
    long notificationsCreated7d,
    long notificationsDelivered7d,
    long notificationsFailed7d,
    long pendingNotifications
) {}
```

### Use Cases

- Notification system health monitoring
- Delivery success rate tracking
- Failed notification investigation
- Queue backlog monitoring

---

## Implementation Guide

### Adding New Analytics

To add new analytics to a module:

1. **Define SPI Interface** in `shared/spi/analytics/`:
```java
public interface MyModuleAnalyticsService {
    MyModuleAnalyticsSnapshot captureSnapshot();
}
```

2. **Create Snapshot Record**:
```java
public record MyModuleAnalyticsSnapshot(
    long metric1,
    long metric2,
    double metric3
) {}
```

3. **Implement Service** in module's `service/impl/`:
```java
@Service
@RequiredArgsConstructor
public class MyModuleAnalyticsServiceImpl implements MyModuleAnalyticsService {
    private final MyRepository myRepository;

    @Override
    public MyModuleAnalyticsSnapshot captureSnapshot() {
        // Query repositories and calculate metrics
        return new MyModuleAnalyticsSnapshot(/* metrics */);
    }
}
```

4. **Integrate with Admin Service**:
Update `AdminServiceImpl.getDashboardStatistics()` to include new analytics.

### Best Practices

1. **Keep Snapshots Immutable**: Use records for snapshot DTOs
2. **Efficient Queries**: Use optimized repository methods (e.g., `countBy...`)
3. **Time Windows**: Standardize on 7d, 30d, 6m periods where applicable
4. **Error Handling**: Gracefully handle missing data (return 0 or empty rather than throwing)
5. **Performance**: Cache analytics snapshots if queries are expensive
6. **Testing**: Write unit tests for analytics calculation logic

### Security Considerations

- Admin analytics accessible only to users with `ADMIN` domain
- Organization-scoped analytics for organization admins
- Instructor analytics filtered by instructor's own data
- Student analytics private to the student

---

## Future Analytics Enhancements

### Planned Features

1. **Time-Series Analytics**: Historical trend data over configurable periods
2. **Predictive Analytics**: ML-based forecasting for enrollment, completion
3. **Custom Dashboards**: User-configurable analytics widgets
4. **Export Capabilities**: CSV/PDF report generation
5. **Real-Time Analytics**: WebSocket-based live metric updates
6. **Comparative Analytics**: Benchmarking against platform averages

### API Endpoints (Proposed)

- `GET /api/v1/analytics/{domain}/time-series?metric={name}&period={duration}`
- `GET /api/v1/analytics/{domain}/export?format={csv|pdf}`
- `GET /api/v1/analytics/{domain}/benchmarks`

---

## Troubleshooting

### Common Issues

**Analytics returning zeros:**
- Check that analytics service implementation is annotated with `@Service`
- Verify repository methods are returning correct counts
- Ensure database contains test data

**Slow analytics queries:**
- Add database indexes on frequently queried columns
- Consider implementing analytics caching layer
- Review query execution plans

**Missing analytics in response:**
- Verify module's analytics service is injected in `AdminServiceImpl`
- Check for exceptions in service logs
- Validate JSON serialization of snapshot records

---

## References

- Admin Dashboard Guide: `docs/guides/AdminDashboardDevelopmentGuide.md`
- Spring Modulith Documentation: https://docs.spring.io/spring-modulith/
- Analytics SPI Source: `src/main/java/apps/sarafrika/elimika/shared/spi/analytics/`
- Admin Aggregation: `src/main/java/apps/sarafrika/elimika/tenancy/services/impl/AdminServiceImpl.java`
