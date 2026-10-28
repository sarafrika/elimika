package apps.sarafrika.elimika.shared.spi;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Programme titles for modules that only hold a programme uuid (the skills wallet names its source). */
public interface ProgramTitleLookupService {

    /** Title per known programme uuid; unknown uuids are left out. */
    Map<UUID, String> findTitles(Collection<UUID> programUuids);
}
