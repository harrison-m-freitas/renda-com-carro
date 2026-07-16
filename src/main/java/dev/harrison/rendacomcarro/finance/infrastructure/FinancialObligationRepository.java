package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialObligationRepository
    extends JpaRepository<FinancialObligation, UUID> {

    List<FinancialObligation> findAllByOrderByCreatedAtDesc();

    @Query("""
        select o
        from FinancialObligation o
        join fetch o.vehicle v
        where o.status = dev.harrison.rendacomcarro.finance.domain.ObligationStatus.ACTIVE
          and o.mode = dev.harrison.rendacomcarro.finance.domain.ObligationMode.FLEXIBLE
          and o.monthlyTarget is not null
          and o.monthlyTarget > 0
          and v.id = :vehicleId
        order by o.createdAt asc
        """)
    List<FinancialObligation> findActiveFlexibleTargets(
        @Param("vehicleId") UUID vehicleId
    );
}
