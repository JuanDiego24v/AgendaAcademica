package proyecto.personal.dto;

import proyecto.personal.model.Examen;

public record ExamenDto(
        Long id,
        String nombre,
        String fecha,
        Double nota,
        Double porcentaje,
        String estado,
        CursoDto curso
) {
    public static ExamenDto from(Examen e) {
        return new ExamenDto(
                e.getId(),
                e.getNombre(),
                e.getFecha().toString(),
                e.getNota(),
                e.getPorcentaje(),
                e.getEstado(),
                CursoDto.from(e.getCurso())
        );
    }
}
