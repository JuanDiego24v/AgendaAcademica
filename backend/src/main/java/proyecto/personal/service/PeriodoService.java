package proyecto.personal.service;

import proyecto.personal.model.Periodo;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.PeriodoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PeriodoService {

    private final PeriodoRepository periodoRepository;
    private final UsuarioService usuarioService;

    public PeriodoService(PeriodoRepository periodoRepository,
                          UsuarioService usuarioService) {
        this.periodoRepository = periodoRepository;
        this.usuarioService = usuarioService;
    }

    // CREAR PERIODO

    public Periodo crear(String nombre, LocalDate fechaInicio, int semanas) {

        Usuario usuario = usuarioService.obtenerUsuarioActual();

        // Desactivar el periodo activo actual si existe
        periodoRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .ifPresent(p -> {
                    p.setActivo(false);
                    periodoRepository.save(p);
                });

        Periodo nuevo = new Periodo();
        nuevo.setUsuario(usuario);
        nuevo.setNombre(nombre);
        nuevo.setFechaInicio(fechaInicio);
        nuevo.setSemanas(semanas);
        nuevo.setActivo(true);

        return periodoRepository.save(nuevo);
    }

    // OBTENER ACTIVO

    public Optional<Periodo> obtenerActivo() {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        return periodoRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
    }

    public Periodo obtenerActivoOrThrow() {
        return obtenerActivo()
                .orElseThrow(() -> new RuntimeException("Sin periodo activo"));
    }

    // LISTAR

    public List<Periodo> listar() {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        return periodoRepository.findByUsuarioIdOrderByIdDesc(usuario.getId());
    }
}
