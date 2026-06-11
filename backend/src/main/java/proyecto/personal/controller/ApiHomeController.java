package proyecto.personal.controller;

import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.ExamenDto;
import proyecto.personal.service.ExamenService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/home")
public class ApiHomeController {

    private final ExamenService examenService;

    public ApiHomeController(ExamenService examenService) {
        this.examenService = examenService;
    }

    @GetMapping("/proximos")
    public List<ExamenDto> proximosExamenes() {
        return examenService.listarExamenesDelUsuarioActual().stream()
                .filter(e -> !e.getFecha().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(e -> e.getFecha()))
                .map(ExamenDto::from)
                .toList();
    }
}
