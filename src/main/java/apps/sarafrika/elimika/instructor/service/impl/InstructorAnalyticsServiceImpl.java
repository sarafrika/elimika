package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsSnapshot;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstructorAnalyticsServiceImpl implements InstructorAnalyticsService {

    private final InstructorRepository instructorRepository;
    private final ProfessionalProfileService profileService;

    /** Document counts come from the shared profile documents, which every domain files into. */
    @Override
    public InstructorAnalyticsSnapshot captureSnapshot() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate thirtyDaysAhead = today.plusDays(30);

        long verified = instructorRepository.countByAdminVerified(Boolean.TRUE);
        long pending = instructorRepository.countByAdminVerified(Boolean.FALSE);
        long documentsPendingVerification = profileService.countUnverifiedDocuments();
        long expiringDocuments = profileService.countDocumentsExpiringBetween(today, thirtyDaysAhead);

        return new InstructorAnalyticsSnapshot(
                verified,
                pending,
                documentsPendingVerification,
                expiringDocuments
        );
    }
}
