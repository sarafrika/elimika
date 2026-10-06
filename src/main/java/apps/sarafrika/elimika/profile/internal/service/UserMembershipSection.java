package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserMembership;
import apps.sarafrika.elimika.profile.internal.repository.UserMembershipRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserMembershipDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserMembershipSection extends ProfileSectionSupport<UserMembership, UserMembershipDTO> {

    public UserMembershipSection(UserMembershipRepository repository,
                                 GenericSpecificationBuilder<UserMembership> specificationBuilder,
                                 ApplicationEventPublisher eventPublisher) {
        super(UserMembership.class, ProfileSection.MEMBERSHIPS, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserMembership newEntity() {
        return new UserMembership();
    }

    @Override
    protected boolean apply(UserMembership membership, UserMembershipDTO dto) {
        membership.setOrganizationName(dto.organizationName());
        membership.setMembershipNumber(dto.membershipNumber());
        membership.setStartDate(dto.startDate());
        membership.setEndDate(dto.endDate());
        membership.setIsActive(dto.isActive() == null ? Boolean.TRUE : dto.isActive());
        return false;
    }

    @Override
    protected String naturalKey(UserMembershipDTO dto) {
        return key(dto.organizationName(), dto.membershipNumber());
    }

    @Override
    protected String naturalKeyOf(UserMembership membership) {
        return key(membership.getOrganizationName(), membership.getMembershipNumber());
    }

    @Override
    protected UserMembershipDTO toDto(UserMembership m) {
        return new UserMembershipDTO(m.getUuid(), m.getUserUuid(), m.getOrganizationName(), m.getMembershipNumber(),
                m.getStartDate(), m.getEndDate(), m.getIsActive(), m.getCreatedDate(), m.getCreatedBy(),
                m.getLastModifiedDate(), m.getLastModifiedBy());
    }
}
