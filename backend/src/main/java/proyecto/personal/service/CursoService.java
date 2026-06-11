package proyecto.personal.service;

import proyecto.personal.model.Curso;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.CursoRepository;
import proyecto.personal.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;

    public CursoService(CursoRepository cursoRepository,
                        UsuarioRepository usuarioRepository) {

        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // USUARIO ACTUAL

    private Usuario obtenerUsuarioActual() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return usuarioRepository
                .findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // GUARDAR CURSO

    public Curso guardar(Curso curso) {

        Usuario usuario = obtenerUsuarioActual();

        curso.setUsuario(usuario);

        return cursoRepository.save(curso);
    }

    // ACTUALIZAR CURSO

    public Curso actualizar(Long id, String nombre) {

        Usuario usuario = obtenerUsuarioActual();

        Curso cursoExistente = cursoRepository
                .findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() ->
                        new RuntimeException("Curso no encontrado"));

        cursoExistente.setNombre(nombre);

        return cursoRepository.save(cursoExistente);
    }

    // LISTAR CURSOS DEL USUARIO ACTUAL

    public List<Curso> listarCursosDelUsuarioActual() {

        Usuario usuario = obtenerUsuarioActual();

        return cursoRepository.findByUsuarioId(usuario.getId());
    }

    // BUSCAR CURSO POR ID

    public Optional<Curso> buscarPorId(Long id) {
        return cursoRepository.findById(id);
    }

    // BUSCAR CURSO DEL USUARIO ACTUAL

    public Curso buscarCursoDelUsuarioActual(Long id) {

        Usuario usuario = obtenerUsuarioActual();

        return cursoRepository
                .findByIdAndUsuarioId(id, usuario.getId())
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

        curso.setUsuario(usuario);

        cursoRepository.save(curso);
    }
}