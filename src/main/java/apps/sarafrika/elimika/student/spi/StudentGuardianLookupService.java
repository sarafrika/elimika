package apps.sarafrika.elimika.student.spi;

import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import java.util.List;
import java.util.UUID;

/**
 * Lookup service for guardian-linked students.
 */
public interface StudentGuardianLookupService {

    List<GuardianStudentAccess> findActiveGuardianStudents(UUID guardianUserUuid);

    record GuardianStudentAccess(UUID studentUuid, GuardianShareScope shareScope) { }
}
