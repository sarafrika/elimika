package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserEducation;
import apps.sarafrika.elimika.profile.internal.repository.UserEducationRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserEducationDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserEducationSection extends ProfileSectionSupport<UserEducation, UserEducationDTO> {

    public UserEducationSection(UserEducationRepository repository,
                                GenericSpecificationBuilder<UserEducation> specificationBuilder,
                                ApplicationEventPublisher eventPublisher) {
        super(UserEducation.class, ProfileSection.EDUCATION, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserEducation newEntity() {
        return new UserEducation();
    }

    @Override
    protected boolean apply(UserEducation education, UserEducationDTO dto) {
        education.setQualification(dto.qualification());
        education.setFieldOfStudy(dto.fieldOfStudy());
        education.setSchoolName(dto.schoolName());
        education.setStartYear(dto.startYear());
        education.setYearCompleted(dto.yearCompleted());
        education.setCertificateNumber(dto.certificateNumber());
        return false;
    }

    @Override
    protected String naturalKey(UserEducationDTO dto) {
        return key(dto.qualification(), dto.schoolName(), dto.yearCompleted());
    }

    @Override
    protected String naturalKeyOf(UserEducation education) {
        return key(education.getQualification(), education.getSchoolName(), education.getYearCompleted());
    }

    @Override
    protected UserEducationDTO toDto(UserEducation e) {
        return new UserEducationDTO(e.getUuid(), e.getUserUuid(), e.getQualification(), e.getFieldOfStudy(),
                e.getSchoolName(), e.getStartYear(), e.getYearCompleted(), e.getCertificateNumber(),
                e.getCreatedDate(), e.getCreatedBy(), e.getLastModifiedDate(), e.getLastModifiedBy());
    }
}
