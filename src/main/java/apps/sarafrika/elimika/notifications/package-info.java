/**
 * The Notifications Module provides centralized notification services for the Elimika platform.
 * 
 * This module follows Spring Modulith principles and provides:
 * - Email notification services using existing mail infrastructure
 * - Event-driven communication with other modules
 * - User preference management for notification delivery
 * - Template-based notification content rendering
 * - Extensible architecture for future notification channels
 * 
 * The module exposes its services through well-defined APIs while keeping implementation
 * details internal. Other modules can request notifications by publishing domain events
 * or through the NotificationService SPI.
 * 
 * Key Components:
 * - NotificationService: Main service interface for sending notifications
 * - NotificationEvent: Domain events for inter-module communication  
 * - UserNotificationPreferences: User preference management
 * - EmailTemplateService: Template rendering and management
 * 
 * @since 2.7.0
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"shared"})
package apps.sarafrika.elimika.notifications;
