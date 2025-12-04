package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassDefinitionLookupServiceImpl implements ClassDefinitionLookupService {

    private final ClassDefinitionServiceInterface classDefinitionService;

    @Override
    public Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        try {
            ClassDefinitionDTO dto = classDefinitionService.getClassDefinition(classDefinitionUuid);
            if (dto == null) {
                return Optional.empty();
            }
            return Optional.of(new ClassDefinitionSnapshot(
                    dto.uuid(),
                    dto.courseUuid(),
                    dto.title(),
                    dto.description(),
                    dto.trainingFee(),
                    dto.classVisibility(),
                    dto.maxParticipants(),
                    dto.allowWaitlist()
            ));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }
}
