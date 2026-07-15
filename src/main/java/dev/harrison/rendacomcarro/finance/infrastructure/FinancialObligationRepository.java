package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialObligationRepository extends JpaRepository<FinancialObligation, UUID> {
    @EntityGraph(attributePaths = {"vehicle", "acquisitionPlan"})
    List<FinancialObligation> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"vehicle", "acquisitionPlan"})
    List<FinancialObligation> findAllByAcquisitionPlanIdOrderByCreatedAtAsc(UUID acquisitionPlanId);

    @EntityGraph(attributePaths = {"vehicle", "acquisitionPlan"})
    Optional<FinancialObligation> findOneById(UUID id);
}
