package proyecto.personal.service;

import jakarta.transaction.Transactional;
import proyecto.personal.model.Usuario;
import proyecto.personal.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REGISTRO

    public Usuario registrar(Usuario usuario) {

        usuario.setPassword(
                passwordEncoder.encode(usuario.getPassword())
        );

        usuario.setEnabled(true);

        return usuarioRepository.save(usuario);
    }

    // BÚSQUEDAS

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean existeUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    // USUARIO ACTUAL

    private String obtenerUsernameActual() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    public Usuario obtenerUsuarioActual() {

        return usuarioRepository
                .findByUsername(obtenerUsernameActual())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // SISTEMA DE EVALUACIÓN

    @Transactional
    public void actualizarSistemaEvaluacion(
            Double notaMaxima,
            Double notaMinimaAprobatoria
    ) {

        Usuario usuario = obtenerUsuarioActual();

        if (notaMaxima == null || notaMaxima <= 0) {
            throw new RuntimeException("La nota máxima debe ser mayor a 0");
        }

        if (notaMinimaAprobatoria == null
                || notaMinimaAprobatoria < 0
                || notaMinimaAprobatoria > notaMaxima) {

            throw new RuntimeException(
                    "La nota mínima aprobatoria debe estar entre 0 y la nota máxima"
            );
        }

        usuario.setNotaMaxima(notaMaxima);
        usuario.setNotaMinimaAprobatoria(notaMinimaAprobatoria);

        usuarioRepository.save(usuario);
    }

    // CAMBIAR PASSWORD

    @Transactional
    public void cambiarPassword(
            String username,
            String passwordActual,
            String nuevaPassword
    ) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar contraseña actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña es muy corta");
        }

        // Encriptar nueva contraseña
        String passwordEncriptada = passwordEncoder.encode(nuevaPassword);

        usuario.setPassword(passwordEncriptada);

        usuarioRepository.save(usuario);
    }

    public void guardar(Usuario usuario) {
        usuarioRepository.save(usuario);
    }
}