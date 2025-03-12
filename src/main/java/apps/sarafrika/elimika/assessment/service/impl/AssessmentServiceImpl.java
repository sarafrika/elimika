package apps.sarafrika.elimika.assessment.service.impl;

import apps.sarafrika.elimika.assessment.config.exceptions.AssessmentNotFoundException;
import apps.sarafrika.elimika.assessment.dto.request.CreateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AssessmentResponseDTO;
import apps.sarafrika.elimika.assessment.event.CreateAssessmentEvent;
import apps.sarafrika.elimika.assessment.event.UpdateAssessmentEvent;
import apps.sarafrika.elimika.assessment.persistence.Assessment;
import apps.sarafrika.elimika.assessment.persistence.AssessmentFactory;
import apps.sarafrika.elimika.assessment.persistence.AssessmentRepository;
import apps.sarafrika.elimika.assessment.service.AssessmentService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AssessmentServiceImpl implements AssessmentService {

    private static final String ERROR_ASSESSMENT_NOT_FOUND = "Assessment not found.";
    private static final String ASSESSMENT_FOUND_SUCCESS = "Assessment retrieved successfully.";
    private static final String ASSESSMENT_CREATED_SUCCESS = "Assessment persisted successfully.";
    private static final String ASSESSMENT_UPDATED_SUCCESS = "Assessment updated successfully.";

    private final ApplicationEventPublisher eventPublisher;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<AssessmentResponseDTO> findAssessmentsByCourse(Long courseId, Pageable pageable) {

        Page<AssessmentResponseDTO> assessmentsPage = assessmentRepository.findAllByCourseId(courseId, pageable).stream()
                .map(AssessmentResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(assessmentsPage.getContent(), assessmentsPage.getNumber(), assessmentsPage.getSize(),
                assessmentsPage.getTotalPages(), assessmentsPage.getTotalElements(), HttpStatus.OK.value(), ASSESSMENT_FOUND_SUCCESS);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<AssessmentResponseDTO> findAssessmentsByLesson(Long lessonId, Pageable pageable) {

        Page<AssessmentResponseDTO> assessmentsPage = assessmentRepository.findAllByLessonId(lessonId, pageable).stream()
                .map(AssessmentResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(assessmentsPage.getContent(), assessmentsPage.getNumber(), assessmentsPage.getSize(),
                assessmentsPage.getTotalPages(), assessmentsPage.getTotalElements(), HttpStatus.OK.value(), ASSESSMENT_FOUND_SUCCESS);

    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<AssessmentResponseDTO> findAssessment(Long id) {

        Assessment assessment = findAssessmentById(id);

        AssessmentResponseDTO assessmentResponseDTO = AssessmentResponseDTO.from(assessment);

        return new ResponseDTO<>(assessmentResponseDTO, HttpStatus.OK.value(), ASSESSMENT_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createAssessment(CreateAssessmentRequestDTO createAssessmentRequestDTO) {

        Assessment assessment = AssessmentFactory.create(createAssessmentRequestDTO);

        eventPublisher.publishEvent(new CreateAssessmentEvent(assessment, createAssessmentRequestDTO));

        assessmentRepository.save(assessment);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), ASSESSMENT_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateAssessment(Long id, UpdateAssessmentRequestDTO updateAssessmentRequestDTO) {

        final Assessment assessment = findAssessmentById(id);

        AssessmentFactory.update(assessment, updateAssessmentRequestDTO);

        eventPublisher.publishEvent(new UpdateAssessmentEvent(assessment, updateAssessmentRequestDTO));

        assessmentRepository.save(assessment);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), ASSESSMENT_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteAssessment(Long id) {
        Assessment assessment = findAssessmentById(id);

        assessmentRepository.delete(assessment);
    }

    private Assessment findAssessmentById(Long id) {

        return assessmentRepository.findById(id).orElseThrow(() -> new AssessmentNotFoundException(ERROR_ASSESSMENT_NOT_FOUND));
    }
}
