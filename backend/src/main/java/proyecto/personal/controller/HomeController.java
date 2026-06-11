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

import java.util.Comparator;


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
            @RequestParam(required = false) Double nota,
            @RequestParam Double porcentaje,
            @RequestParam Long cursoId,
            @RequestParam(defaultValue = "PENDIENTE") String estado) {
        examenService.guardar(nombre, fecha, nota, porcentaje, cursoId, estado);
        return "redirect:/home";
    }

    @GetMapping("/calendario")
    public String calendario(Model model) {
        List<Examen> examenes = examenService.listarExamenesDelUsuarioActual()
                .stream()
                .filter(e -> !e.getFecha().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Examen::getFecha))
                .toList();
        model.addAttribute("todosExamenes", examenes);
        return "calendario";
    }

    @GetMapping("/api/examenes/eventos")
    @ResponseBody
    public List<EventoDTO> listarEventos() {
        return examenService.obtenerEventos();
    }
}