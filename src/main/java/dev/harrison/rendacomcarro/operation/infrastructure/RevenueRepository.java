package dev.harrison.rendacomcarro.operation.infrastructure;

import dev.harrison.rendacomcarro.operation.domain.Revenue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevenueRepository extends JpaRepository<Revenue, UUID> {
    boolean existsByPlatformIdAndExternalReference(UUID platformId, String externalReference);

    List<Revenue> findAllByShiftIdOrderByCompetenceDateDesc(UUID shiftId);

    @Query("select sum(r.netAmount) from Revenue r where r.competenceDate = :date")
    Optional<BigDecimal> sumNetByCompetenceDate(@Param("date") LocalDate date);

    @Query("select sum(r.netAmount) from Revenue r where r.receivedDate = :date")
    Optional<BigDecimal> sumNetByReceivedDate(@Param("date") LocalDate date);

    @Query("select sum(r.netAmount) from Revenue r where r.competenceDate between :start and :end")
    Optional<BigDecimal> sumNetByCompetenceDateBetween(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("select sum(r.netAmount) from Revenue r where r.receivedDate between :start and :end")
    Optional<BigDecimal> sumNetByReceivedDateBetween(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}
