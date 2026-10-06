package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserPortfolioItem;
import apps.sarafrika.elimika.profile.internal.repository.UserPortfolioItemRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserPortfolioItemDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserPortfolioSection extends ProfileSectionSupport<UserPortfolioItem, UserPortfolioItemDTO> {

    public UserPortfolioSection(UserPortfolioItemRepository repository,
                                GenericSpecificationBuilder<UserPortfolioItem> specificationBuilder,
                                ApplicationEventPublisher eventPublisher) {
        super(UserPortfolioItem.class, ProfileSection.PORTFOLIO, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserPortfolioItem newEntity() {
        return new UserPortfolioItem();
    }

    @Override
    protected boolean apply(UserPortfolioItem item, UserPortfolioItemDTO dto) {
        item.setTitle(dto.title());
        item.setItemType(dto.itemType());
        item.setLinkUrl(dto.linkUrl());
        item.setCompletedOn(dto.completedOn());
        item.setDescription(dto.description());
        return false;
    }

    @Override
    protected String naturalKey(UserPortfolioItemDTO dto) {
        return key(dto.title(), dto.itemType());
    }

    @Override
    protected String naturalKeyOf(UserPortfolioItem item) {
        return key(item.getTitle(), item.getItemType());
    }

    @Override
    protected UserPortfolioItemDTO toDto(UserPortfolioItem i) {
        return new UserPortfolioItemDTO(i.getUuid(), i.getUserUuid(), i.getTitle(), i.getItemType(), i.getLinkUrl(),
                i.getCompletedOn(), i.getDescription(), i.getCreatedDate(), i.getCreatedBy(),
                i.getLastModifiedDate(), i.getLastModifiedBy());
    }
}
