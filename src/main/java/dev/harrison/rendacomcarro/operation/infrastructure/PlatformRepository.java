package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.Platform; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface PlatformRepository extends JpaRepository<Platform,UUID>{ Optional<Platform> findByCode(String code); List<Platform> findAllByActiveTrueOrderByNameAsc(); }
