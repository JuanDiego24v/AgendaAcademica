package proyecto.personal.controller;

import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.ExamenDto;
import proyecto.personal.service.ExamenService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/calendario")
public class ApiCalendarioController {

    private final ExamenService examenService;

    public ApiCalendarioController(ExamenService examenService) {
        this.examenService = examenService;
    }

    @GetMapping
    public List<ExamenDto> todos() {
        return examenService.listarExamenesDelPeriodoActual().stream()
                .filter(e -> !e.getFecha().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(e -> e.getFecha()))
                .map(ExamenDto::from)
                .toList();
    }
}
