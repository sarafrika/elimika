/**
 * The Resourcing Module manages bookable physical resources owned by organisations.
 *
 * This module is responsible for the "where and with what" of learning delivery:
 * venues (classrooms, labs) with seat capacity and equipment pools (laptops,
 * instruments) with countable quantity. Each resource carries a Google-Calendar-style
 * availability calendar of recurring open hours and blackout windows, plus time-slot
 * bookings.
 *
 * Key Features:
 * - Resource Registry: Organisations register venues and equipment pools, optionally per branch
 * - Availability Rules: Recurring OPEN_HOURS and recurring or one-off BLACKOUT rules per resource
 * - Bookings: HOLD reservations placed while a marketplace job recruits, converted to
 *   CONFIRMED bookings when the class is created, released when the job is cancelled or expires
 * - Conflict Detection: Venue exclusivity and equipment quantity aggregation over overlapping windows
 *
 * Module Boundaries:
 * - Owns: organisation resources, availability rules, resource bookings
 * - Does Not Own: marketplace jobs, class definitions, scheduled instances (referenced by UUID only)
 *
 * The module exposes {@code ResourceBookingService} and {@code ResourceLookupService}
 * through the resourcing-spi named interface; the classes and timetabling modules call
 * these synchronously so booking state changes stay atomic with job/class state changes.
 *
 * @since 2.96.0
 */
@ApplicationModule(
        displayName = "Resourcing",
        allowedDependencies = {
                "shared",
                "tenancy :: tenancy-spi"
        }
)
package apps.sarafrika.elimika.resourcing;

import org.springframework.modulith.ApplicationModule;
