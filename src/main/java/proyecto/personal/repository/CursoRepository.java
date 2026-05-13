package proyecto.personal.repository;

import proyecto.personal.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByUsuarioId(Long usuarioId);

    Optional<Curso> findByIdAndUsuarioId(Long id, Long usuarioId);
}