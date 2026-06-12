package proyecto.personal.repository;

import proyecto.personal.model.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

    List<Periodo> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    Optional<Periodo> findByUsuarioIdAndActivoTrue(Long usuarioId);

    Optional<Periodo> findByIdAndUsuarioId(Long id, Long usuarioId);
}
