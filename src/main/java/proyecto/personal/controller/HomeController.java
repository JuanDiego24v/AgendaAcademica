package proyecto.personal.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import proyecto.personal.DTOs.EventoDTO;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;


@Controller
public class HomeController {

    private final CursoService cursoService;
    private final ExamenService examenService;

    public HomeController(CursoService cursoService, ExamenService examenService) {
        this.cursoService = cursoService;
        this.examenService = examenService;
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {

        model.addAttribute("usuario", authentication.getPrincipal());
        model.addAttribute("curso", new Curso());
        model.addAttribute("examen", new Examen());
        model.addAttribute("cursos", cursoService.listarCursosDelUsuarioActual());
        model.addAttribute("proximosExamenes", examenService.obtenerProximosExamenes());
        
        return "home";
    }

    @PostMapping("/home/curso")
    public String crearCurso(@ModelAttribute Curso curso, Authentication authentication) {

        cursoService.guardarParaUsuario(curso, authentication.getName());
        return "redirect:/home";
    }

    @PostMapping("/home/examen")
        public String crearExamen(
            @RequestParam String nombre,
            @RequestParam LocalDate fecha,
            @RequestParam Double nota,
            @RequestParam Double porcentaje,
            @RequestParam Long cursoId
        ) {
        examenService.guardar(nombre, fecha, nota, porcentaje, cursoId);
        return "redirect:/home";
    }

    @GetMapping("/api/examenes")
    @ResponseBody
    public List<EventoDTO> listarEventos() {
        return examenService.obtenerEventos();
    }
}