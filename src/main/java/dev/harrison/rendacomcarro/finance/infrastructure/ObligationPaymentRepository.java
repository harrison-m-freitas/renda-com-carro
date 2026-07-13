package dev.harrison.rendacomcarro.finance.infrastructure;

import dev.harrison.rendacomcarro.finance.domain.ObligationPayment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObligationPaymentRepository extends JpaRepository<ObligationPayment, UUID> {
    boolean existsByObligationIdAndExternalReference(UUID id, String ref);

    List<ObligationPayment> findAllByObligationIdOrderByPaymentDateDesc(UUID id);

    List<ObligationPayment> findAllByPaymentDateBetween(LocalDate start, LocalDate end);
}
