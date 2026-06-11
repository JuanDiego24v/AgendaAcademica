package proyecto.personal.dto;

public record ExamenRequest(
        String nombre,
        String fecha,
        Double porcentaje,
        String estado,
        Double nota,
        Long cursoId
) {}
