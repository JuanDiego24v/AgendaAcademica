package proyecto.personal.DTOs;

public class EventoDTO {

    private String title;
    private String start;
    private String curso;
    private Double porcentaje;
    private String estado;

    public EventoDTO(String title, String start, String curso, Double porcentaje, String estado) {
        this.title = title;
        this.start = start;
        this.curso = curso;
        this.porcentaje = porcentaje;
        this.estado = estado;
    }

    public String getTitle()      { return title; }
    public String getStart()      { return start; }
    public String getCurso()      { return curso; }
    public Double getPorcentaje() { return porcentaje; }
    public String getEstado()     { return estado; }
}
