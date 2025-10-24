package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.repository.InstructorDocumentRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstructorAnalyticsServiceImpl implements InstructorAnalyticsService {

    private final InstructorRepository instructorRepository;
    private final InstructorDocumentRepository instructorDocumentRepository;

    @Override
    public InstructorAnalyticsSnapshot captureSnapshot() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAhead = today.plusDays(30);

        long verified = instructorRepository.countByAdminVerified(Boolean.TRUE);
        long pending = instructorRepository.countByAdminVerified(Boolean.FALSE);
        long documentsPendingVerification = instructorDocumentRepository.countByIsVerifiedFalse();
        long expiringDocuments = instructorDocumentRepository
                .countExpiringBetweenExcludingStatus(today, thirtyDaysAhead, DocumentStatus.EXPIRED);

        return new InstructorAnalyticsSnapshot(
                verified,
                pending,
                documentsPendingVerification,
                expiringDocuments
        );
    }
}
