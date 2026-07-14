package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
import dev.harrison.rendacomcarro.finance.domain.ObligationInstallment;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObligationInstallmentRepository
    extends JpaRepository<ObligationInstallment, UUID> {

    List<ObligationInstallment> findAllByObligationIdOrderByDueDateAsc(UUID id);

    List<ObligationInstallment> findAllByObligationIdAndStatusNotOrderByDueDateAsc(
        UUID obligationId,
        InstallmentStatus status
    );

    @Query("""
        select
            i.id as installmentId,
            o.id as obligationId,
            v.id as vehicleId,
            v.name as vehicleName,
            o.creditor as creditor,
            i.dueDate as dueDate,
            i.expectedAmount as expectedAmount,
            i.paidAmount as paidAmount
        from ObligationInstallment i
        join i.obligation o
        join o.vehicle v
        where o.status = dev.harrison.rendacomcarro.finance.domain.ObligationStatus.ACTIVE
          and v.id in :vehicleIds
          and i.dueDate <= :monthEnd
          and i.status not in (
              dev.harrison.rendacomcarro.finance.domain.InstallmentStatus.PAID,
              dev.harrison.rendacomcarro.finance.domain.InstallmentStatus.CANCELLED
          )
        order by i.dueDate asc, i.sequenceNumber asc
        """)
    List<InstallmentSuggestionProjection> findSuggestionCandidates(
        @Param("vehicleIds") Set<UUID> vehicleIds,
        @Param("monthEnd") LocalDate monthEnd
    );
}
