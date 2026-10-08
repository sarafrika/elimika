package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCreatorProfileBridgeTest {

    @Mock
    private CourseCreatorRepository repository;

    @InjectMocks
    private CourseCreatorProfileBridge bridge;

    @Test
    void scopesAnInFilterToEveryOwningUser() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        when(repository.findByUuidIn(Set.of(first, second)))
                .thenReturn(List.of(owned(first, firstUser), owned(second, secondUser)));

        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(Map.of(
                "course_creator_uuid_in", first + ", " + second, "skill_name", "Java"));

        assertThat(scoped.userUuids()).containsExactlyInAnyOrder(firstUser, secondUser);
        assertThat(scoped.params()).containsExactly(Map.entry("skill_name", "Java"));
    }

    @Test
    void scopesAnExactFilterToItsUser() {
        UUID courseCreatorUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        when(repository.findByUuidIn(Set.of(courseCreatorUuid))).thenReturn(List.of(owned(courseCreatorUuid, userUuid)));

        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(Map.of("courseCreatorUuid", courseCreatorUuid.toString()));

        assertThat(scoped.userUuids()).containsExactly(userUuid);
    }

    @Test
    void scopesUnknownUuidsToNoUsers() {
        UUID unknown = UUID.randomUUID();
        when(repository.findByUuidIn(Set.of(unknown))).thenReturn(List.of());

        assertThat(bridge.scope(Map.of("course_creator_uuid_in", unknown.toString())).userUuids()).isEmpty();
    }

    @Test
    void scopesABlankInFilterToNoUsersWithoutQuerying() {
        assertThat(bridge.scope(Map.of("course_creator_uuid_in", " , ")).userUuids()).isEmpty();
        verify(repository, never()).findByUuidIn(any());
    }

    @Test
    void boundsToAllOwnersWithoutAFilter() {
        UUID userUuid = UUID.randomUUID();
        when(repository.findAllUserUuids()).thenReturn(List.of(userUuid));

        assertThat(bridge.scope(Map.of()).userUuids()).containsExactly(userUuid);
    }

    @Test
    void rejectsOtherKeyOperatorsAndMalformedUuids() {
        assertThatThrownBy(() -> bridge.scope(Map.of("course_creator_uuid_ne", UUID.randomUUID().toString())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.scope(Map.of("course_creator_uuid_in", "not-a-uuid")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CourseCreator owned(UUID uuid, UUID userUuid) {
        CourseCreator entity = new CourseCreator();
        entity.setUuid(uuid);
        entity.setUserUuid(userUuid);
        return entity;
    }
}
