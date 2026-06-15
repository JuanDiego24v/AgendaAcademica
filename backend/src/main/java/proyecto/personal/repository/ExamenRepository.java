package proyecto.personal.repository;

import proyecto.personal.model.Examen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    List<Examen> findByCursoPeriodoUsuarioId(Long usuarioId);

    List<Examen> findByCursoId(Long cursoId);

    Optional<Examen> findByIdAndCursoPeriodoUsuarioId(Long id, Long usuarioId);

    List<Examen> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);

    List<Examen> findByCursoPeriodoId(Long periodoId);
}