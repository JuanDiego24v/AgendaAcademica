package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.PerfilDto;
import proyecto.personal.dto.PeriodoDto;
import proyecto.personal.model.Periodo;
import proyecto.personal.model.Usuario;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;
import proyecto.personal.service.PeriodoService;
import proyecto.personal.service.UsuarioService;

import java.util.Optional;

@RestController
@RequestMapping("/api/perfil")
public class ApiPerfilController {

    private final UsuarioService usuarioService;
    private final PeriodoService periodoService;
    private final CursoService cursoService;
    private final ExamenService examenService;

    public ApiPerfilController(UsuarioService usuarioService,
                               PeriodoService periodoService,
                               CursoService cursoService,
                               ExamenService examenService) {
        this.usuarioService = usuarioService;
        this.periodoService = periodoService;
        this.cursoService = cursoService;
        this.examenService = examenService;
    }

    @GetMapping
    public ResponseEntity<PerfilDto> obtenerPerfil() {

        Usuario usuario = usuarioService.obtenerUsuarioActual();
        Optional<Periodo> periodoActivo = periodoService.obtenerActivo();

        long totalCursos = periodoActivo
                .map(p -> (long) cursoService.listarCursosDelUsuarioActual().size())
                .orElse(0L);

        long totalExamenes = (long) examenService.listarExamenesDelPeriodoActual().size();

        PeriodoDto periodoDto = periodoActivo.map(PeriodoDto::from).orElse(null);

        return ResponseEntity.ok(new PerfilDto(
                usuario.getUsername(),
                usuario.getEmail(),
                periodoDto,
                totalCursos,
                totalExamenes
        ));
    }
}
