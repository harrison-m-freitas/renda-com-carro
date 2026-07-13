package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.Trip; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository; public interface TripRepository extends JpaRepository<Trip,UUID>{}
