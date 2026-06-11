package proyecto.personal.dto;

import java.util.List;

public record ExamenesPageResponse(
        List<ExamenDto> examenes,
        List<CursoDto> cursos,
        Long cursoSeleccionado,
        Double notaMaxima,
        Double notaMinimaAprobatoria,
        Double promedioCurso,
        Double porcentajeEvaluado,
        Double porcentajePendiente
) {}
