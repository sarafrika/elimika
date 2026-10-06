package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserDocument;
import apps.sarafrika.elimika.profile.internal.model.UserOwnedEntity;
import apps.sarafrika.elimika.profile.internal.repository.UserDocumentRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserEducationRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserExperienceRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserMembershipRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserOwnedRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class UserDocumentSection extends ProfileSectionSupport<UserDocument, UserDocumentDTO> {

    private final UserDocumentRepository documentRepository;
    private final UserEducationRepository educationRepository;
    private final UserExperienceRepository experienceRepository;
    private final UserMembershipRepository membershipRepository;
    private final MediaStorageService mediaStorageService;

    public UserDocumentSection(UserDocumentRepository repository,
                               GenericSpecificationBuilder<UserDocument> specificationBuilder,
                               ApplicationEventPublisher eventPublisher,
                               UserEducationRepository educationRepository,
                               UserExperienceRepository experienceRepository,
                               UserMembershipRepository membershipRepository,
                               MediaStorageService mediaStorageService) {
        super(UserDocument.class, ProfileSection.DOCUMENTS, repository, specificationBuilder, eventPublisher);
        this.documentRepository = repository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.membershipRepository = membershipRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Override
    protected UserDocument newEntity() {
        return new UserDocument();
    }

    @Override
    protected void prepareNew(UserDocument document, UserDocumentDTO dto) {
        if (dto.filePath() == null || dto.filePath().isBlank()) {
            throw new IllegalArgumentException("A profile document needs an uploaded file");
        }
        document.setOriginalFilename(dto.originalFilename() == null ? dto.storedFilename() : dto.originalFilename());
        document.setStoredFilename(dto.storedFilename() == null ? dto.filePath() : dto.storedFilename());
        document.setFilePath(dto.filePath());
        document.setFileSizeBytes(dto.fileSizeBytes() == null ? 0L : dto.fileSizeBytes());
        document.setMimeType(dto.mimeType() == null ? "application/octet-stream" : dto.mimeType());
        document.setFileHash(dto.fileHash());
        document.setUploadDate(LocalDateTime.now(ZoneOffset.UTC));
        document.setIsVerified(Boolean.FALSE);
        document.setStatus(DocumentStatus.PENDING);
    }

    @Override
    protected boolean apply(UserDocument document, UserDocumentDTO dto) {
        UUID owner = document.getUserUuid();
        document.setDocumentTypeUuid(dto.documentTypeUuid() != null ? dto.documentTypeUuid() : document.getDocumentTypeUuid());
        document.setEducationUuid(requireOwnedLink(educationRepository, dto.educationUuid(), owner, "education"));
        document.setExperienceUuid(requireOwnedLink(experienceRepository, dto.experienceUuid(), owner, "experience"));
        document.setMembershipUuid(requireOwnedLink(membershipRepository, dto.membershipUuid(), owner, "membership"));
        document.setTitle(dto.title() != null ? dto.title() : document.getTitle());
        document.setDescription(dto.description());
        document.setExpiryDate(dto.expiryDate());
        return false;
    }

    @Override
    protected void afterDelete(UserDocument document) {
        mediaStorageService.delete(document.getFilePath() != null ? document.getFilePath() : document.getStoredFilename());
    }

    @Override
    protected void recordVerdict(UserDocument document, WalletVerificationStatus status, String notes,
                                 String verifiedBy, LocalDateTime at) {
        boolean verified = status == WalletVerificationStatus.VERIFIED;
        document.setIsVerified(verified);
        document.setStatus(verified ? DocumentStatus.APPROVED : DocumentStatus.REJECTED);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(at);
        document.setVerificationNotes(notes);
    }

    @Override
    protected String naturalKey(UserDocumentDTO dto) {
        return dto.filePath() == null ? null : key(dto.filePath());
    }

    @Override
    protected String naturalKeyOf(UserDocument document) {
        return key(document.getFilePath());
    }

    @Override
    protected UserDocumentDTO toDto(UserDocument d) {
        return new UserDocumentDTO(d.getUuid(), d.getUserUuid(), d.getDocumentTypeUuid(), d.getEducationUuid(),
                d.getExperienceUuid(), d.getMembershipUuid(), d.getOriginalFilename(), d.getStoredFilename(),
                d.getFilePath(), d.getFileSizeBytes(), d.getMimeType(), d.getFileHash(), d.getTitle(),
                d.getDescription(), d.getUploadDate(), d.getIsVerified(), d.getVerifiedBy(), d.getVerifiedAt(),
                d.getVerificationNotes(), d.getStatus(), d.getExpiryDate(), d.getCreatedDate(), d.getCreatedBy(),
                d.getLastModifiedDate(), d.getLastModifiedBy());
    }

    @Transactional(readOnly = true)
    public boolean ownsFile(UUID userUuid, String filePath) {
        return userUuid != null && filePath != null && documentRepository.existsByUserUuidAndFilePath(userUuid, filePath);
    }

    @Transactional(readOnly = true)
    public long countUnverified() {
        return documentRepository.countByIsVerifiedFalse();
    }

    @Transactional(readOnly = true)
    public long countExpiringBetween(LocalDate start, LocalDate end) {
        return documentRepository.countExpiringBetween(start, end, DocumentStatus.EXPIRED);
    }

    @Transactional(readOnly = true)
    public long countVerified(UUID userUuid) {
        return documentRepository.countByUserUuidAndIsVerifiedTrue(userUuid);
    }

    private static <L extends UserOwnedEntity> UUID requireOwnedLink(UserOwnedRepository<L> repository, UUID linkUuid,
                                                                      UUID ownerUuid, String label) {
        if (linkUuid == null) {
            return null;
        }
        boolean owned = repository.findByUuid(linkUuid)
                .map(link -> link.getUserUuid() != null && link.getUserUuid().equals(ownerUuid))
                .orElse(false);
        if (!owned) {
            throw new IllegalArgumentException("The linked " + label + " record does not belong to this profile");
        }
        return linkUuid;
    }
}
