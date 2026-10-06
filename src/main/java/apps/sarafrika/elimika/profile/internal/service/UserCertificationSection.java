package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserCertification;
import apps.sarafrika.elimika.profile.internal.repository.UserCertificationRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserCertificationDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserCertificationSection extends ProfileSectionSupport<UserCertification, UserCertificationDTO> {

    public UserCertificationSection(UserCertificationRepository repository,
                                    GenericSpecificationBuilder<UserCertification> specificationBuilder,
                                    ApplicationEventPublisher eventPublisher) {
        super(UserCertification.class, ProfileSection.CERTIFICATIONS, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserCertification newEntity() {
        return new UserCertification();
    }

    @Override
    protected boolean apply(UserCertification certification, UserCertificationDTO dto) {
        boolean claimChanged = changed(certification.getCertificationName(), dto.certificationName())
                || changed(certification.getIssuingOrganization(), dto.issuingOrganization())
                || changed(certification.getCredentialId(), dto.credentialId())
                || changed(certification.getCredentialUrl(), dto.credentialUrl());
        certification.setCertificationName(dto.certificationName());
        certification.setIssuingOrganization(dto.issuingOrganization());
        certification.setIssuedDate(dto.issuedDate());
        certification.setExpiryDate(dto.expiryDate());
        certification.setCredentialId(dto.credentialId());
        certification.setCredentialUrl(dto.credentialUrl());
        certification.setDescription(dto.description());
        certification.setCredentialType(dto.credentialType());
        return claimChanged;
    }

    @Override
    protected String naturalKey(UserCertificationDTO dto) {
        return key(dto.certificationName(), dto.issuingOrganization(), dto.credentialId());
    }

    @Override
    protected String naturalKeyOf(UserCertification c) {
        return key(c.getCertificationName(), c.getIssuingOrganization(), c.getCredentialId());
    }

    @Override
    protected UserCertificationDTO toDto(UserCertification c) {
        return new UserCertificationDTO(c.getUuid(), c.getUserUuid(), c.getCertificationName(),
                c.getIssuingOrganization(), c.getIssuedDate(), c.getExpiryDate(), c.getCredentialId(),
                c.getCredentialUrl(), c.getDescription(), c.getCredentialType(), c.getVerificationStatus(),
                c.getVerifiedAt(), c.getVerificationNotes(), c.getCreatedDate(), c.getCreatedBy(),
                c.getLastModifiedDate(), c.getLastModifiedBy());
    }
}
