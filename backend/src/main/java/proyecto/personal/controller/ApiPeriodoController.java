package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.PeriodoDto;
import proyecto.personal.dto.PeriodoRequest;
import proyecto.personal.service.PeriodoService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/periodos")
public class ApiPeriodoController {

    private final PeriodoService periodoService;

    public ApiPeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    @PostMapping
    public ResponseEntity<PeriodoDto> crear(@RequestBody PeriodoRequest request) {
        LocalDate fechaInicio = LocalDate.parse(request.fechaInicio());
        return ResponseEntity.ok(
                PeriodoDto.from(periodoService.crear(request.nombre(), fechaInicio, request.semanas()))
        );
    }

    @GetMapping("/activo")
    public ResponseEntity<PeriodoDto> obtenerActivo() {
        return periodoService.obtenerActivo()
                .map(PeriodoDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PeriodoDto>> listar() {
        List<PeriodoDto> periodos = periodoService.listar().stream()
                .map(PeriodoDto::from)
                .toList();
        return ResponseEntity.ok(periodos);
    }
}
