package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorReviewFactory;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.instructor.service.InstructorReviewService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorReviewServiceImpl implements InstructorReviewService {

    private final InstructorReviewRepository instructorReviewRepository;
    private final EnrollmentRepository enrollmentRepository;

    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Enrollment with ID %s not found";

    @Override
    public InstructorReviewDTO createReview(InstructorReviewDTO reviewDTO) {
        Enrollment enrollment = enrollmentRepository.findByUuid(reviewDTO.enrollmentUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, reviewDTO.enrollmentUuid())));

        if (!enrollment.getStudentUuid().equals(reviewDTO.studentUuid())) {
            throw new IllegalArgumentException("Review student_uuid does not match enrollment student.");
        }

        if (instructorReviewRepository.existsByInstructorUuidAndEnrollmentUuid(
                reviewDTO.instructorUuid(), reviewDTO.enrollmentUuid())) {
            throw new IllegalStateException("A review for this instructor and enrollment already exists.");
        }

        InstructorReview entity = InstructorReviewFactory.toEntity(reviewDTO);
        InstructorReview saved = instructorReviewRepository.save(entity);
        return InstructorReviewFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorReviewDTO> getReviewsForInstructor(UUID instructorUuid) {
        return instructorReviewRepository.findByInstructorUuid(instructorUuid)
                .stream()
                .map(InstructorReviewFactory::toDTO)
                .collect(Collectors.toList());
    }
}
