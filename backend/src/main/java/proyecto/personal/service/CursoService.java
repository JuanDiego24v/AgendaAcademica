package proyecto.personal.service;

import proyecto.personal.model.Curso;
import proyecto.personal.model.Periodo;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.CursoRepository;
import proyecto.personal.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoService periodoService;

    public CursoService(CursoRepository cursoRepository,
                        UsuarioRepository usuarioRepository,
                        PeriodoService periodoService) {

        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.periodoService = periodoService;
    }

    // GUARDAR CURSO

    public Curso guardar(Curso curso) {

        Periodo periodo = periodoService.obtenerActivoOrThrow();

        curso.setPeriodo(periodo);

        return cursoRepository.save(curso);
    }

    // ACTUALIZAR CURSO

    public Curso actualizar(Long id, String nombre) {

        Periodo periodo = periodoService.obtenerActivoOrThrow();

        Curso cursoExistente = cursoRepository
                .findByIdAndPeriodoId(id, periodo.getId())
                .orElseThrow(() ->
                        new RuntimeException("Curso no encontrado"));

        cursoExistente.setNombre(nombre);

        return cursoRepository.save(cursoExistente);
    }

    // LISTAR CURSOS DEL USUARIO ACTUAL

    public List<Curso> listarCursosDelUsuarioActual() {

        Periodo periodo = periodoService.obtenerActivoOrThrow();

        return cursoRepository.findByPeriodoId(periodo.getId());
    }

    // BUSCAR CURSO POR ID

    public Optional<Curso> buscarPorId(Long id) {
        return cursoRepository.findById(id);
    }

    // BUSCAR CURSO DEL USUARIO ACTUAL

    public Curso buscarCursoDelUsuarioActual(Long id) {

        Periodo periodo = periodoService.obtenerActivoOrThrow();

        return cursoRepository
                .findByIdAndPeriodoId(id, periodo.getId())
                .orElseThrow(() ->
                        new RuntimeException("Curso no encontrado o no autorizado"));
    }

    // ELIMINAR CURSO

    public void eliminar(Long id) {

        Curso curso = buscarCursoDelUsuarioActual(id);

        cursoRepository.delete(curso);
    }

    // GUARDAR PARA USUARIO (registro u otros casos)

    public void guardarParaUsuario(Curso curso, String username) {

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

        Periodo periodo = periodoService.obtenerActivo()
                .orElseThrow(() -> new RuntimeException("Sin periodo activo para el usuario"));

        curso.setPeriodo(periodo);

        cursoRepository.save(curso);
    }
}
