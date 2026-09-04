package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import apps.sarafrika.elimika.course.repository.AssignmentAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
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
 * The assignment an attachment is deleted through is what its guard checked, so the attachment
 * must actually belong to it.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAttachmentServiceImplTest {

    @Mock
    private AssignmentAttachmentRepository attachmentRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private MediaStorageService mediaStorageService;

    private AssignmentAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentAttachmentServiceImpl(
                attachmentRepository, assignmentRepository, mediaStorageService);
    }

    @Test
    void deleteRemovesAnAttachmentFiledUnderTheNamedAssignment() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID attachmentUuid = UUID.randomUUID();

        when(attachmentRepository.findByUuid(attachmentUuid))
                .thenReturn(Optional.of(attachment(attachmentUuid, assignmentUuid)));

        service.deleteAttachment(assignmentUuid, attachmentUuid);

        verify(mediaStorageService).delete("assignments/brief.pdf");
        verify(attachmentRepository).deleteByUuid(attachmentUuid);
    }

    @Test
    void deleteRefusesAnAttachmentBelongingToAnotherAssignment() {
        UUID ownAssignmentUuid = UUID.randomUUID();
        UUID foreignAttachmentUuid = UUID.randomUUID();

        when(attachmentRepository.findByUuid(foreignAttachmentUuid))
                .thenReturn(Optional.of(attachment(foreignAttachmentUuid, UUID.randomUUID())));

        assertThatThrownBy(() -> service.deleteAttachment(ownAssignmentUuid, foreignAttachmentUuid))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mediaStorageService, never()).delete(anyString());
        verify(attachmentRepository, never()).deleteByUuid(foreignAttachmentUuid);
    }

    private AssignmentAttachment attachment(UUID uuid, UUID assignmentUuid) {
        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setUuid(uuid);
        attachment.setAssignmentUuid(assignmentUuid);
        attachment.setStoredFilename("assignments/brief.pdf");
        return attachment;
    }
}
