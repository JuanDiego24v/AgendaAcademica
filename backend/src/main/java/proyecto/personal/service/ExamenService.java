package proyecto.personal.service;

import proyecto.personal.DTOs.EventoDTO;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.ExamenRepository;
import proyecto.personal.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final CursoService cursoService;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoService periodoService;

    public ExamenService(
            ExamenRepository examenRepository,
            CursoService cursoService,
            UsuarioRepository usuarioRepository,
            PeriodoService periodoService) {

        this.examenRepository = examenRepository;
        this.cursoService = cursoService;
        this.usuarioRepository = usuarioRepository;
        this.periodoService = periodoService;
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
            Long cursoId,
            String estado) {

        Usuario usuario = obtenerUsuarioActual();
        Curso curso = cursoService.buscarCursoDelUsuarioActual(cursoId);

        if ("COMPLETADO".equals(estado)) {
            validarNota(nota, usuario);
        }
        validarPorcentaje(porcentaje);
        validarPorcentajeTotal(cursoId, porcentaje);

        Examen examen = new Examen();
        examen.setNombre(nombre);
        examen.setFecha(fecha);
        examen.setNota("COMPLETADO".equals(estado) ? nota : null);
        examen.setPorcentaje(porcentaje);
        examen.setEstado(estado != null ? estado : "PENDIENTE");
        examen.setCurso(curso);

        return examenRepository.save(examen);
    }

    // ACTUALIZAR NOTA

    public void actualizarNota(Long examenId, Double nuevaNota) {

        Usuario usuario = obtenerUsuarioActual();

        Examen examen = examenRepository
                .findByIdAndCursoPeriodoUsuarioId(examenId, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no autorizado"));

        validarNota(nuevaNota, usuario);

        examen.setNota(nuevaNota);

        examenRepository.save(examen);
    }

    // CONSULTAS

    public List<Examen> listarExamenesDelUsuarioActual() {

        Usuario usuario = obtenerUsuarioActual();
        return examenRepository.findByCursoPeriodoUsuarioId(usuario.getId());
    }

    public List<Examen> listarExamenesDelPeriodoActual() {

        return periodoService.obtenerActivo()
                .map(p -> examenRepository.findByCursoPeriodoId(p.getId()))
                .orElse(List.of());
    }

    public List<Examen> listarPorCurso(Long cursoId) {

        return examenRepository.findByCursoId(cursoId);
    }

    public Optional<Examen> buscarPorId(Long id) {

        Usuario usuario = obtenerUsuarioActual();

        return examenRepository
                .findByIdAndCursoPeriodoUsuarioId(id, usuario.getId());
    }

    public Examen buscarExamenDelUsuarioActual(Long id) {

        Usuario usuario = obtenerUsuarioActual();

        return examenRepository
                .findByIdAndCursoPeriodoUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no autorizado"));
    }

    // ELIMINAR

    public void eliminar(Long id) {

        Examen examen = buscarExamenDelUsuarioActual(id);
        examenRepository.delete(examen);
    }

    // PROMEDIO PONDERADO (solo COMPLETADO)

    public Double calcularPromedioPorCurso(Long cursoId) {

        return listarPorCurso(cursoId).stream()
                .filter(e -> "COMPLETADO".equals(e.getEstado()) && e.getNota() != null)
                .mapToDouble(e -> e.getNota() * (e.getPorcentaje() / 100.0))
                .sum();
    }

    // PORCENTAJE EVALUADO (solo COMPLETADO)

    public Double calcularPorcentajeEvaluado(Long cursoId) {

        return listarPorCurso(cursoId).stream()
                .filter(e -> "COMPLETADO".equals(e.getEstado()))
                .mapToDouble(Examen::getPorcentaje)
                .sum();
    }

    // PORCENTAJE PENDIENTE (solo PENDIENTE)

    public Double calcularPorcentajePendiente(Long cursoId) {

        return listarPorCurso(cursoId).stream()
                .filter(e -> "PENDIENTE".equals(e.getEstado()))
                .mapToDouble(Examen::getPorcentaje)
                .sum();
    }

    // NOTA NECESARIA PARA APROBAR

    public Double calcularNotaNecesariaParaAprobar(Long cursoId) {

        Usuario usuario = obtenerUsuarioActual();

        double promedioActual = calcularPromedioPorCurso(cursoId);

        double porcentajePendiente = calcularPorcentajePendiente(cursoId);

        if (porcentajePendiente <= 0) return 0.0;

        double notaNecesaria = (usuario.getNotaMinimaAprobatoria() - promedioActual)
                / (porcentajePendiente / 100);

        return Math.max(0, notaNecesaria);
    }

    // EDITAR DATOS DEL EXAMEN

    public void editarDatosExamen(
            Long id,
            String nombre,
            LocalDate fecha,
            Double porcentaje,
            Double nota,
            String estado) {

        Examen examen = buscarExamenDelUsuarioActual(id);
        Usuario usuario = obtenerUsuarioActual();

        validarPorcentaje(porcentaje);

        if ("COMPLETADO".equals(estado) && nota != null) {
            validarNota(nota, usuario);
        }

        examen.setNombre(nombre);
        examen.setFecha(fecha);
        examen.setPorcentaje(porcentaje);
        examen.setEstado(estado);
        examen.setNota("COMPLETADO".equals(estado) ? nota : null);

        examenRepository.save(examen);
    }

    // EVENTOS PARA CALENDARIO

    public List<EventoDTO> obtenerEventos() {

        return listarExamenesDelUsuarioActual().stream()
                .map(ex -> new EventoDTO(
                        ex.getNombre(),
                        ex.getFecha().toString(),
                        ex.getCurso().getNombre(),
                        ex.getPorcentaje(),
                        ex.getEstado()))
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
