package proyecto.personal.repository;

import proyecto.personal.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByPeriodoId(Long periodoId);

    Optional<Curso> findByIdAndPeriodoId(Long id, Long periodoId);
}