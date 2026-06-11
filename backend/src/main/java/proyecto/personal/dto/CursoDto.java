package proyecto.personal.dto;

import proyecto.personal.model.Curso;

public record CursoDto(Long id, String nombre) {
    public static CursoDto from(Curso c) {
        return new CursoDto(c.getId(), c.getNombre());
    }
}
