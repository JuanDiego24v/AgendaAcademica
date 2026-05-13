package proyecto.personal.controller;

import proyecto.personal.model.Usuario;
import proyecto.personal.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // LOGIN
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // FORM REGISTRO
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "register";
    }

    // PROCESAR REGISTRO
    @PostMapping("/register")
    public String register(@ModelAttribute Usuario usuario, Model model) {

        if (usuarioService.existeUsername(usuario.getUsername())) {
            model.addAttribute("error", "El usuario ya existe");
            return "register";
        }

        if (usuarioService.existeEmail(usuario.getEmail())) {
            model.addAttribute("error", "El email ya está registrado");
            return "register";
        }

        usuarioService.registrar(usuario);
        return "redirect:/login";
    }
}