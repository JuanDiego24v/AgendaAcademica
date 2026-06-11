package proyecto.personal.controller;

import proyecto.personal.model.Curso;
import proyecto.personal.service.CursoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    // 📌 Mostrar tabla con modal incluido
    @GetMapping
    public String listarCursos(Model model) {
        model.addAttribute("cursos", cursoService.listarCursosDelUsuarioActual());
        model.addAttribute("curso", new Curso()); // para el modal
        return "cursos";
    }

    // 📌 Guardar (nuevo)
    @PostMapping("/guardar")
    public String guardarCurso(@ModelAttribute Curso curso) {

        if (curso.getId() != null) {
            // Validación de seguridad: verificar que el curso pertenece al usuario
            cursoService.buscarCursoDelUsuarioActual(curso.getId());
        }

        cursoService.guardar(curso);
        return "redirect:/cursos";
    }

    // Guardar edición
    @PostMapping("/editar")
    public String editarCurso(
            @RequestParam Long id,
            @RequestParam String nombre) {

        cursoService.actualizar(id, nombre);

        return "redirect:/cursos";
    }

    // 📌 Eliminar (seguro)
    @PostMapping("/eliminar/{id}")
    public String eliminarCurso(@PathVariable Long id) {

        cursoService.eliminar(id);

        return "redirect:/cursos";
    }
}