package proyecto.personal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.AuthRequest;
import proyecto.personal.dto.AuthResponse;
import proyecto.personal.dto.RegisterRequest;
import proyecto.personal.model.Usuario;
import proyecto.personal.security.JwtUtil;
import proyecto.personal.service.UsuarioService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    public ApiAuthController(AuthenticationManager authManager,
                             JwtUtil jwtUtil,
                             UsuarioService usuarioService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
            String token = jwtUtil.generateToken(auth.getName());
            return ResponseEntity.ok(new AuthResponse(token, auth.getName()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (usuarioService.existeUsername(req.username())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El usuario ya existe"));
        }
        if (usuarioService.existeEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El email ya está registrado"));
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(req.username());
        usuario.setEmail(req.email());
        usuario.setPassword(req.password());
        usuarioService.registrar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
