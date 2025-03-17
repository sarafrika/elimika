package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.PrerequisiteGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrerequisiteGroupItemRepository extends JpaRepository<PrerequisiteGroupItem, Long> {

    List<PrerequisiteGroupItem> findAllByPrerequisiteGroupId(Long prerequisiteGroupId);

    void deleteAllByPrerequisiteGroupId(Long prerequisiteGroupId);
}
