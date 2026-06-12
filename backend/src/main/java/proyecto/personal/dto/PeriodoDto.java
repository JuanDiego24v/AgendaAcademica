package proyecto.personal.dto;

import proyecto.personal.model.Periodo;

public record PeriodoDto(Long id, String nombre, String fechaInicio, Integer semanas, boolean activo) {

    public static PeriodoDto from(Periodo p) {
        return new PeriodoDto(
                p.getId(),
                p.getNombre(),
                p.getFechaInicio().toString(),
                p.getSemanas(),
                p.isActivo()
        );
    }
}
