package proyecto.personal.controller;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import proyecto.personal.service.UsuarioService;

@Controller
@RequestMapping("/change-password")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Mostrar formulario
    @GetMapping
    public String mostrarFormulario() {
        return "change-password";
    }

    // Procesar cambio
    @PostMapping
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "change-password";
        }

        try {
            usuarioService.cambiarPassword(
                    principal.getName(),
                    currentPassword,
                    newPassword
            );

            // 🔐 Logout automático
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
            }

            return "redirect:/login?passwordChanged";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "change-password";
        }
    }
}