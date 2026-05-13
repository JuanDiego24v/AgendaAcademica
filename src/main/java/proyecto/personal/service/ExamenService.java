package proyecto.personal.service;

import proyecto.personal.DTOs.EventoDTO;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.ExamenRepository;
import proyecto.personal.repository.CursoRepository;
import proyecto.personal.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;

    public ExamenService(
            ExamenRepository examenRepository,
            CursoRepository cursoRepository,
            UsuarioRepository usuarioRepository) {

        this.examenRepository = examenRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // USUARIO ACTUAL

    private String obtenerUsernameActual() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    private Usuario obtenerUsuarioActual() {
        return usuarioRepository
                .findByUsername(obtenerUsernameActual())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Curso obtenerCursoDelUsuario(Long cursoId) {

        Usuario usuario = obtenerUsuarioActual();

        return cursoRepository
                .findByIdAndUsuarioId(cursoId, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Curso no autorizado"));
    }

    // VALIDACIONES

    private void validarNota(Double nota, Usuario usuario) {

        if (nota == null) {
            throw new IllegalArgumentException("La nota no puede ser nula");
        }

        if (nota < 0 || nota > usuario.getNotaMaxima()) {
            throw new IllegalArgumentException(
                    "La nota debe estar entre 0 y " + usuario.getNotaMaxima());
        }
    }

    private void validarPorcentaje(Double porcentaje) {

        if (porcentaje == null || porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "El porcentaje debe estar entre 1 y 100");
        }
    }

    private void validarPorcentajeTotal(Long cursoId, Double nuevoPorcentaje) {

        List<Examen> examenes = listarPorCurso(cursoId);

        double sumaActual = examenes.stream()
                .mapToDouble(Examen::getPorcentaje)
                .sum();

        if (sumaActual + nuevoPorcentaje > 100) {
            throw new IllegalArgumentException(
                    "El total de porcentajes no puede superar 100%");
        }
    }

    // GUARDAR EXAMEN

    public Examen guardar(
            String nombre,
            LocalDate fecha,
            Double nota,
            Double porcentaje,
            Long cursoId) {

        Usuario usuario = obtenerUsuarioActual();
        Curso curso = obtenerCursoDelUsuario(cursoId);

        validarNota(nota, usuario);
        validarPorcentaje(porcentaje);
        validarPorcentajeTotal(cursoId, porcentaje);

        Examen examen = new Examen();
        examen.setNombre(nombre);
        examen.setFecha(fecha);
        examen.setNota(nota);
        examen.setPorcentaje(porcentaje);
        examen.setUsuario(usuario);
        examen.setCurso(curso);

        return examenRepository.save(examen);
    }

    // ACTUALIZAR NOTA (slider)

    public void actualizarNota(Long examenId, Double nuevaNota) {

        Usuario usuario = obtenerUsuarioActual();

        Examen examen = examenRepository
                .findByIdAndUsuarioId(examenId, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no autorizado"));

        validarNota(nuevaNota, usuario);

        examen.setNota(nuevaNota);

        examenRepository.save(examen);
    }

    // CONSULTAS

    public List<Examen> listarExamenesDelUsuarioActual() {

        Usuario usuario = obtenerUsuarioActual();
        return examenRepository.findByUsuarioId(usuario.getId());
    }

    public List<Examen> listarPorCurso(Long cursoId) {

        Usuario usuario = obtenerUsuarioActual();

        return examenRepository
                .findByCursoIdAndUsuarioId(cursoId, usuario.getId());
    }

    public Optional<Examen> buscarPorId(Long id) {

        Usuario usuario = obtenerUsuarioActual();

        return examenRepository
                .findByIdAndUsuarioId(id, usuario.getId());
    }

    public Examen buscarExamenDelUsuarioActual(Long id) {

        Usuario usuario = obtenerUsuarioActual();

        return examenRepository
                .findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no autorizado"));
    }

    // ELIMINAR

    public void eliminar(Long id) {

        Examen examen = buscarExamenDelUsuarioActual(id);
        examenRepository.delete(examen);
    }

    // PROMEDIO PONDERADO

    public Double calcularPromedioPorCurso(Long cursoId) {

        List<Examen> examenes = listarPorCurso(cursoId);

        if (examenes.isEmpty()) {
            return 0.0;
        }

        return examenes.stream()
                .mapToDouble(e -> e.getNota() * (e.getPorcentaje() / 100.0))
                .sum();
    }

    // PORCENTAJE EVALUADO

    public Double calcularPorcentajeEvaluado(Long cursoId) {

        List<Examen> examenes = listarPorCurso(cursoId);

        return examenes.stream()
                .mapToDouble(Examen::getPorcentaje)
                .sum();
    }

    // NOTA NECESARIA PARA APROBAR

    public Double calcularNotaNecesariaParaAprobar(Long cursoId) {

        Usuario usuario = obtenerUsuarioActual();

        double promedioActual = calcularPromedioPorCurso(cursoId);

        double porcentajeEvaluado = calcularPorcentajeEvaluado(cursoId);

        double porcentajeRestante = 100 - porcentajeEvaluado;

        if (porcentajeRestante <= 0) {
            return 0.0;
        }

        double notaNecesaria = (usuario.getNotaMinimaAprobatoria() - promedioActual)
                / (porcentajeRestante / 100);

        return Math.max(0, notaNecesaria);
    }

    public void editarDatosExamen(
            Long id,
            String nombre,
            LocalDate fecha,
            Double porcentaje) {

        Examen examen = buscarExamenDelUsuarioActual(id);

        validarPorcentaje(porcentaje);

        examen.setNombre(nombre);
        examen.setFecha(fecha);
        examen.setPorcentaje(porcentaje);

        examenRepository.save(examen);
    }

    // EVENTOS PARA CALENDARIO(EXÁMENES)
    public List<EventoDTO> obtenerEventos() {

        return listarExamenesDelUsuarioActual().stream()
                .map(ex -> new EventoDTO(
                        ex.getNombre(),
                        ex.getFecha().toString(),
                        ex.getCurso().getNombre(),
                        ex.getPorcentaje()))
                .toList();
    }

    // NOTIFICACIONES
    public List<Examen> obtenerProximosExamenes() {

        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(7);

        return examenRepository
                .findByFechaBetweenOrderByFechaAsc(hoy, limite);
    }
}