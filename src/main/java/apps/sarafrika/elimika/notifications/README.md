# Elimika Notifications Module

## Overview

The Notifications module provides centralized notification services for the Elimika platform, following Spring Modulith principles. This module handles email notifications with a foundation for future multi-channel expansion (SMS, push notifications, in-app alerts).

## Architecture

### Spring Modulith Design
- **API Package**: Public interfaces and events (`api/`)
- **Internal Package**: Implementation details (`internal/`) 
- **Model Package**: JPA entities and repositories (`model/`)
- **Service Package**: Core services (`service/`)
- **Template Package**: Email template processing (`template/`)
- **Config Package**: Module configuration (`config/`)

### Key Components

1. **NotificationService** (SPI): Main interface for sending notifications
2. **NotificationEvent**: Base interface for all notification events
3. **EmailNotificationService**: Email delivery implementation
4. **EmailTemplateService**: Thymeleaf template processing
5. **NotificationEventListener**: Event-driven message processing
6. **UserNotificationPreferences**: User preference management

## Features

### ✅ MVP Features (Email Only)
- **Email Notifications**: Professional HTML templates using existing mail infrastructure
- **User Preferences**: Category-based notification controls with quiet hours
- **Event-Driven**: Async processing with Spring Modulith events
- **Template System**: Thymeleaf templates with consistent design
- **Delivery Tracking**: Comprehensive logging and status tracking
- **Smart Routing**: Preference-based delivery decisions

### 📋 Notification Types
- **Learning Journey**: Course enrollment, progress milestones, completion certificates
- **Assignment Workflow**: Due reminders, submission confirmations, grading notifications
- **Instructor Tools**: New submissions, grading reminders, student enrollment alerts
- **Administrative**: Invitations, account management, system notifications

### 🎨 Email Templates
- **assignment-due-reminder.html**: Urgent and standard assignment reminders
- **assignment-graded.html**: Grade notifications with feedback display
- **course-enrollment-welcome.html**: Welcome emails with course information
- **new-assignment-submission.html**: Instructor notifications for submissions

## Usage

### Publishing Notification Events

Other modules can trigger notifications by publishing events:

```java
@Component
public class CourseService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void enrollStudent(UUID studentId, UUID courseId) {
        // Business logic...
        
        // Publish notification event
        CourseEnrollmentWelcomeEvent event = new CourseEnrollmentWelcomeEvent(
            null, // Auto-generated notification ID
            studentId,
            student.getEmail(),
            student.getName(),
            courseId,
            course.getName(),
            course.getDescription(),
            instructor.getName(),
            course.getStartDate(),
            course.getDurationWeeks(),
            course.getImageUrl(),
            course.getOrganizationId(),
            null // Auto-generated timestamp
        );
        
        eventPublisher.publishEvent(event);
    }
}
```

### Direct Service Usage

For programmatic notification sending:

```java
@Autowired
private NotificationService notificationService;

public void sendCustomNotification() {
    NotificationEvent event = // create event...
    
    CompletableFuture<NotificationResult> result = notificationService.sendNotification(event);
    
    result.thenAccept(res -> {
        if (res.isSuccessful()) {
            log.info("Notification sent successfully");
        }
    });
}
```

## Configuration

### Email Templates
Templates are located in `src/main/resources/templates/email/` and follow the existing design system with:
- Consistent branding and colors
- Mobile-responsive design
- Professional layouts with proper typography
- Thymeleaf variable binding for dynamic content

### User Preferences
Users can control notifications per category:
- **Learning Progress & Achievements**: Course milestones, progress updates
- **Assignments & Grading**: Due dates, submissions, feedback
- **Course Management**: Instructor-specific notifications
- **Social Learning**: Peer activities, community interactions
- **System & Administrative**: Account, invitations, security alerts

### Database Schema
The module creates two main tables:
- `user_notification_preferences`: User preference storage
- `notification_delivery_log`: Delivery tracking and analytics

## Integration Points

### With Existing Systems
- **Email Infrastructure**: Uses existing `JavaMailSender` and mail configuration
- **Template Engine**: Leverages existing Thymeleaf setup
- **User Management**: Integrates with user UUID system
- **Organization Context**: Supports multi-tenant branding

### Event Sources
The module listens for events from:
- **Course Module**: Enrollment, completion, progress events
- **Assignment System**: Due dates, submissions, grading events
- **User Management**: Registration, invitation, role change events
- **Admin Functions**: System notifications and alerts

## Extending the System

### Adding New Notification Types
1. Add enum to `NotificationType`
2. Create event class implementing `NotificationEvent`
3. Create HTML template in `resources/templates/email/`
4. Update database constraints if needed

### Adding New Channels
The architecture supports easy extension:
1. Create channel-specific service (e.g., `SmsNotificationService`)
2. Update `NotificationServiceImpl` routing logic
3. Add channel preferences to `UserNotificationPreferences`
4. Implement delivery tracking

### Template Customization
- Templates use consistent CSS classes and structure
- Variables are passed through `NotificationEvent.getTemplateVariables()`
- Utility functions available for date formatting, URL building, text formatting
- Organization-specific branding supported through template variables

## Monitoring & Analytics

### Delivery Tracking
All notification attempts are logged with:
- Delivery status and timestamps
- Retry counts and error messages
- Template used and channel information
- User and organization context

### Performance Monitoring
- Async processing prevents blocking
- Configurable retry mechanisms
- Circuit breaker patterns (future enhancement)
- Comprehensive logging at all levels

## Future Enhancements

### Planned Features
- **In-App Notifications**: Real-time browser notifications
- **SMS Integration**: Critical alert delivery via SMS
- **Push Notifications**: Mobile app integration
- **Advanced Analytics**: Delivery metrics and user engagement
- **A/B Testing**: Template performance optimization
- **Batching & Digests**: Reduce notification frequency
- **Personalization**: AI-driven content customization

### Scalability Considerations
- Message queue integration for high volume
- Database partitioning for delivery logs
- Caching layer for user preferences
- Load balancing for email sending
- Rate limiting for external APIs

## Development Guidelines

### Testing Strategy
- Unit tests for all service classes
- Integration tests for email delivery
- Template rendering tests
- Mock external dependencies
- Test notification preference scenarios

### Code Quality
- Follow existing code conventions
- Comprehensive error handling
- Detailed logging at appropriate levels
- Performance-conscious async processing
- Security-first approach to user data

This notifications module provides a solid foundation for the Elimika platform's communication needs while maintaining flexibility for future enhancements and integrations.