package apps.sarafrika.elimika.search.internal.state;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchIndexStateRepository extends JpaRepository<SearchIndexState, String> {
}
