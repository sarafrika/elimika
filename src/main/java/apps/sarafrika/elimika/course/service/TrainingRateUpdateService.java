package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDecisionRequest;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateRequest;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Rate updates on approved training applications; {@code parentUuid} is the targeted course or program. */
public interface TrainingRateUpdateService {

    TrainingRateUpdateDTO submitRateUpdate(UUID parentUuid, UUID applicationUuid, TrainingRateUpdateRequest request);

    List<TrainingRateUpdateDTO> getRateUpdates(UUID parentUuid, UUID applicationUuid);

    Page<TrainingRateUpdateDTO> getRateUpdatesForReview(UUID parentUuid,
                                                        Optional<TrainingRateUpdateStatus> status,
                                                        Pageable pageable);

    TrainingRateUpdateDTO approveRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid,
                                            TrainingRateUpdateDecisionRequest request);

    TrainingRateUpdateDTO rejectRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid,
                                           TrainingRateUpdateDecisionRequest request);

    void withdrawRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid);

    /** Rejects any pending update when the application stops being approved; a no-op otherwise. */
    void closePendingRateUpdate(UUID applicationUuid, String reason);
}
