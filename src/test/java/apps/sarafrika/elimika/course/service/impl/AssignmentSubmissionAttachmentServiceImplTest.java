package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The submission an attachment is deleted through is part of its address, not decoration.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionAttachmentServiceImplTest {

    @Mock
    private AssignmentSubmissionAttachmentRepository attachmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private MediaStorageService mediaStorageService;

    private AssignmentSubmissionAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentSubmissionAttachmentServiceImpl(
                attachmentRepository, submissionRepository, mediaStorageService);
    }

    @Test
    void deleteRemovesAnAttachmentFiledUnderTheNamedSubmission() {
        UUID submissionUuid = UUID.randomUUID();
        UUID attachmentUuid = UUID.randomUUID();

        when(attachmentRepository.findByUuid(attachmentUuid))
                .thenReturn(Optional.of(attachment(attachmentUuid, submissionUuid)));

        service.deleteAttachment(submissionUuid, attachmentUuid);

        verify(mediaStorageService).delete("assignments/submissions/report.pdf");
        verify(attachmentRepository).deleteByUuid(attachmentUuid);
    }

    @Test
    void deleteRefusesAnAttachmentBelongingToAnotherLearnersSubmission() {
        UUID ownSubmissionUuid = UUID.randomUUID();
        UUID victimAttachmentUuid = UUID.randomUUID();

        when(attachmentRepository.findByUuid(victimAttachmentUuid))
                .thenReturn(Optional.of(attachment(victimAttachmentUuid, UUID.randomUUID())));

        assertThatThrownBy(() -> service.deleteAttachment(ownSubmissionUuid, victimAttachmentUuid))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mediaStorageService, never()).delete(anyString());
        verify(attachmentRepository, never()).deleteByUuid(victimAttachmentUuid);
    }

    private AssignmentSubmissionAttachment attachment(UUID uuid, UUID submissionUuid) {
        AssignmentSubmissionAttachment attachment = new AssignmentSubmissionAttachment();
        attachment.setUuid(uuid);
        attachment.setSubmissionUuid(submissionUuid);
        attachment.setStoredFilename("assignments/submissions/report.pdf");
        return attachment;
    }
}
