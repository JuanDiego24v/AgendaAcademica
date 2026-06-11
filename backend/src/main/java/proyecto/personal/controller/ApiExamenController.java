package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.DTOs.EventoDTO;
import proyecto.personal.dto.*;
import proyecto.personal.model.Curso;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;
import proyecto.personal.service.UsuarioService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/examenes")
public class ApiExamenController {

    private final ExamenService examenService;
    private final CursoService cursoService;
    private final UsuarioService usuarioService;

    public ApiExamenController(ExamenService examenService,
                               CursoService cursoService,
                               UsuarioService usuarioService) {
        this.examenService = examenService;
        this.cursoService = cursoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/eventos")
    public List<EventoDTO> listarEventos() {
        return examenService.obtenerEventos();
    }

    @GetMapping
    public ExamenesPageResponse pagina(@RequestParam(required = false) Long cursoId) {
        List<Curso> cursos = cursoService.listarCursosDelUsuarioActual().stream()
                .sorted(Comparator.comparing(Curso::getNombre))
                .toList();

        var usuario = usuarioService.obtenerUsuarioActual();

        if (cursos.isEmpty()) {
            return new ExamenesPageResponse(
                    List.of(), List.of(), null,
                    usuario.getNotaMaxima(), usuario.getNotaMinimaAprobatoria(),
                    0.0, 0.0, 0.0);
        }

        if (cursoId == null) cursoId = cursos.get(0).getId();

        List<ExamenDto> examenes = examenService.listarPorCurso(cursoId).stream()
                .map(ExamenDto::from).toList();

        return new ExamenesPageResponse(
                examenes,
                cursos.stream().map(CursoDto::from).toList(),
                cursoId,
                usuario.getNotaMaxima(),
                usuario.getNotaMinimaAprobatoria(),
                examenService.calcularPromedioPorCurso(cursoId),
                examenService.calcularPorcentajeEvaluado(cursoId),
                examenService.calcularPorcentajePendiente(cursoId)
        );
    }

    @PostMapping
    public ExamenDto crear(@RequestBody ExamenRequest req) {
        return ExamenDto.from(examenService.guardar(
                req.nombre(),
                LocalDate.parse(req.fecha()),
                req.nota(),
                req.porcentaje(),
                req.cursoId(),
                req.estado() != null ? req.estado() : "PENDIENTE"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@PathVariable Long id, @RequestBody ExamenRequest req) {
        examenService.editarDatosExamen(
                id,
                req.nombre(),
                LocalDate.parse(req.fecha()),
                req.porcentaje(),
                req.nota(),
                req.estado() != null ? req.estado() : "PENDIENTE"
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        examenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuario/actualizar-sistema")
    public ResponseEntity<Void> actualizarSistema(@RequestBody SistemaRequest req) {
        usuarioService.actualizarSistemaEvaluacion(req.notaMaxima(), req.notaMinimaAprobatoria());
        return ResponseEntity.ok().build();
    }
}
