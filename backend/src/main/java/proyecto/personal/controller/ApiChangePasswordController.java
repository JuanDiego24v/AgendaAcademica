package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import proyecto.personal.dto.ChangePasswordRequest;
import proyecto.personal.service.UsuarioService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiChangePasswordController {

    private final UsuarioService usuarioService;

    public ApiChangePasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> cambiar(@RequestBody ChangePasswordRequest req, Authentication auth) {
        try {
            usuarioService.cambiarPassword(auth.getName(), req.currentPassword(), req.newPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
