package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorRatingSummaryDTO;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.instructor.search.InstructorRatingAggregate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorReviewServiceImplTest {

    @Mock
    private InstructorReviewRepository instructorReviewRepository;

    @InjectMocks
    private InstructorReviewServiceImpl service;

    @Test
    void ratingSummaryIsAggregatedInTheDatabase() {
        UUID instructorUuid = UUID.randomUUID();
        when(instructorReviewRepository.aggregateRatingsByInstructorUuidIn(List.of(instructorUuid)))
                .thenReturn(List.of(new InstructorRatingAggregate(instructorUuid, 4.5, 2L)));

        assertThat(service.getRatingSummary(instructorUuid))
                .isEqualTo(new InstructorRatingSummaryDTO(instructorUuid, 4.5, 2L));
        verify(instructorReviewRepository, never()).findByInstructorUuid(any());
    }

    @Test
    void anUnreviewedInstructorHasNoAverageAndAZeroCount() {
        UUID instructorUuid = UUID.randomUUID();
        when(instructorReviewRepository.aggregateRatingsByInstructorUuidIn(List.of(instructorUuid)))
                .thenReturn(List.of());

        assertThat(service.getRatingSummary(instructorUuid))
                .isEqualTo(new InstructorRatingSummaryDTO(instructorUuid, null, 0L));
    }
}
