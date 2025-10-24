package apps.sarafrika.elimika.timetabling.spi;

/**
 * Enrollment Lookup Service Provider Interface
 * <p>
 * Provides read-only access to enrollment information for other modules.
 * This interface extends the shared enrollment lookup interface to maintain
 * backward compatibility while allowing shared module access.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface EnrollmentLookupService extends apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService {
    // This interface extends the shared version to maintain module boundaries
    // while allowing shared module to use enrollment lookup functionality
}