package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.AcquisitionPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcquisitionPlanRepository extends JpaRepository<AcquisitionPlan, UUID> {
    @EntityGraph(attributePaths = "vehicle")
    List<AcquisitionPlan> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "vehicle")
    Optional<AcquisitionPlan> findOneById(UUID id);
}
