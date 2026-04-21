package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.internal.UserMediaValidationService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import apps.sarafrika.elimika.tenancy.util.UserSpecificationBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private UserDomainRepository userDomainRepository;

    @Mock
    private UserDomainMappingRepository userDomainMappingRepository;

    @Mock
    private UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;

    @Mock
    private TrainingBranchRepository trainingBranchRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private GenericSpecificationBuilder<User> specificationBuilder;

    @Mock
    private UserSpecificationBuilder userSpecificationBuilder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private UserMediaValidationService validationService;

    @Mock
    private NotificationPreferencesService notificationPreferencesService;

    @Mock
    private UserNumberService userNumberService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRemoveStandaloneDomainMappingsWhenDomainRemovalEventIsReceived() {
        UUID userUuid = UUID.randomUUID();
        UUID domainUuid = UUID.randomUUID();
        UserDomain domain = new UserDomain();
        domain.setUuid(domainUuid);
        domain.setDomainName("student");

        UserDomainMapping mapping = new UserDomainMapping();
        mapping.setUserUuid(userUuid);
        mapping.setUserDomainUuid(domainUuid);

        when(userDomainRepository.findByDomainName("student")).thenReturn(Optional.of(domain));
        when(userDomainMappingRepository.findByUserUuidAndUserDomainUuid(userUuid, domainUuid))
                .thenReturn(List.of(mapping));

        userService.removeUserDomain(new UserDomainRemovedEvent(userUuid, "student"));

        verify(userDomainMappingRepository).deleteAll(List.of(mapping));
    }
}
