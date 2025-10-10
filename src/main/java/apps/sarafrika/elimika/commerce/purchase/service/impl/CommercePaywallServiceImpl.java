package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.commerce.purchase.exception.CommercePaymentRequiredException;
import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.commerce.purchase.service.CommercePaywallService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePaywallServiceImpl implements CommercePaywallService {

    private final CommerceAccessService commerceAccessService;
    private final ClassDefinitionService classDefinitionService;

    @Override
    public void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid) {
        if (studentUuid == null) {
            throw new CommercePaymentRequiredException("Student identifier is required to verify payments.");
        }
        if (classDefinitionUuid == null) {
            throw new CommercePaymentRequiredException("Class definition identifier is required to verify payments.");
        }

        ClassDefinitionDTO classDefinition = classDefinitionService.getClassDefinition(classDefinitionUuid);
        UUID courseUuid = classDefinition != null ? classDefinition.courseUuid() : null;

        if (courseUuid != null && !commerceAccessService.hasCourseAccess(studentUuid, courseUuid)) {
            log.debug("Course access not found for student {} and course {}", studentUuid, courseUuid);
            throw new CommercePaymentRequiredException("Course fee must be settled before enrolling in classes.");
        }

        if (!commerceAccessService.hasClassAccess(studentUuid, classDefinitionUuid)) {
            log.debug("Class access not found for student {} and class {}", studentUuid, classDefinitionUuid);
            throw new CommercePaymentRequiredException("Class fee must be settled before enrollment is permitted.");
        }
    }
}
