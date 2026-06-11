package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.CursoDto;
import proyecto.personal.dto.CursoRequest;
import proyecto.personal.model.Curso;
import proyecto.personal.service.CursoService;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class ApiCursoController {

    private final CursoService cursoService;

    public ApiCursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoDto> listar() {
        return cursoService.listarCursosDelUsuarioActual().stream()
                .sorted(Comparator.comparing(Curso::getNombre))
                .map(CursoDto::from)
                .toList();
    }

    @PostMapping
    public CursoDto crear(@RequestBody CursoRequest req) {
        Curso curso = new Curso();
        curso.setNombre(req.nombre());
        return CursoDto.from(cursoService.guardar(curso));
    }

    @PutMapping("/{id}")
    public CursoDto actualizar(@PathVariable Long id, @RequestBody CursoRequest req) {
        return CursoDto.from(cursoService.actualizar(id, req.nombre()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
