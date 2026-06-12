package proyecto.personal.dto;

public record PerfilDto(
        String username,
        String email,
        PeriodoDto periodoActivo,
        long totalCursos,
        long totalExamenes
) {}
