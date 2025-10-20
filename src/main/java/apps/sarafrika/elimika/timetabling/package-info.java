/**
 * Timetabling Module
 * <p>
 * The timetabling module handles class scheduling, student enrollment management,
 * attendance tracking, and conflict detection within the Elimika educational platform.
 * 
 * <h2>Core Capabilities</h2>
 * <ul>
 *   <li><strong>Class Scheduling</strong> - Schedule class instances on the calendar with instructor assignments</li>
 *   <li><strong>Enrollment Management</strong> - Handle student enrollment/cancellation for scheduled classes</li>
 *   <li><strong>Conflict Detection</strong> - Prevent scheduling conflicts for instructors and students</li>
 *   <li><strong>Attendance Tracking</strong> - Record and manage student attendance for completed classes</li>
 *   <li><strong>Status Management</strong> - Automatic status transitions for scheduled instances</li>
 * </ul>
 * 
 * <h2>Module Dependencies</h2>
 * <ul>
 *   <li><strong>Classes Module</strong> - Consumes class definition data and events</li>
 *   <li><strong>Availability Module</strong> - Integrates with instructor availability for conflict detection</li>
 *   <li><strong>Student Module</strong> - Links to student data for enrollment management</li>
 *   <li><strong>Tenancy Module</strong> - Inherits multi-tenancy support</li>
 * </ul>
 * 
 * <h2>Event-Driven Integration</h2>
 * <p>This module publishes events for scheduling activities and listens to events from other modules
 * to maintain data consistency and handle business rule changes automatically.</p>
 * 
 * <h3>Published Events</h3>
 * <ul>
 *   <li>{@code ClassScheduledEventDTO} - When a class is scheduled</li>
 *   <li>{@code StudentEnrolledEventDTO} - When a student enrolls</li>
 *   <li>{@code AttendanceMarkedEventDTO} - When attendance is recorded</li>
 * </ul>
 * 
 * <h3>Consumed Events</h3>
 * <ul>
 *   <li>{@code ClassDefinedEventDTO} - Updates when new classes are defined</li>
 *   <li>{@code ClassDefinitionUpdatedEventDTO} - Synchronizes class definition changes</li>
 *   <li>{@code ClassDefinitionDeactivatedEventDTO} - Handles class deactivation</li>
 *   <li>{@code InstructorAvailabilityChangedEventDTO} - Detects scheduling conflicts</li>
 * </ul>
 * 
 * <h2>Data Model</h2>
 * <ul>
 *   <li>{@link apps.sarafrika.elimika.timetabling.model.ScheduledInstance} - Represents scheduled class instances</li>
 *   <li>{@link apps.sarafrika.elimika.timetabling.model.Enrollment} - Links students to scheduled instances</li>
 * </ul>
 * 
 * <h2>Public API</h2>
 * <p>The module exposes its functionality through the {@link apps.sarafrika.elimika.timetabling.spi.TimetableService}
 * interface, which provides all scheduling and enrollment operations for other modules.</p>
 * 
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Timetabling",
    allowedDependencies = {"shared", "availability :: availability-spi", "student", "tenancy"}
)
package apps.sarafrika.elimika.timetabling;