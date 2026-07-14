package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
import dev.harrison.rendacomcarro.finance.domain.ObligationInstallment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObligationInstallmentRepository
    extends JpaRepository<ObligationInstallment, UUID> {

    List<ObligationInstallment> findAllByObligationIdOrderByDueDateAsc(UUID id);

    List<ObligationInstallment> findAllByObligationIdAndStatusNotOrderByDueDateAsc(
        UUID obligationId,
        InstallmentStatus status
    );
}
