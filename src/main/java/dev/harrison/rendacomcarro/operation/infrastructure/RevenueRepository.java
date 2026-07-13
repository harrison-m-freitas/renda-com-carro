package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.Revenue; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository; public interface RevenueRepository extends JpaRepository<Revenue,UUID>{}
