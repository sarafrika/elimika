package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.shared.spi.ProgramTitleLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramTitleLookupServiceImpl implements ProgramTitleLookupService {

    private final TrainingProgramRepository programRepository;

    @Override
    public Map<UUID, String> findTitles(Collection<UUID> programUuids) {
        if (programUuids == null || programUuids.isEmpty()) {
            return Map.of();
        }
        return programRepository.findByUuidIn(programUuids.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .filter(program -> program.getTitle() != null)
                .collect(Collectors.toMap(TrainingProgram::getUuid, TrainingProgram::getTitle, (a, b) -> a));
    }
}
