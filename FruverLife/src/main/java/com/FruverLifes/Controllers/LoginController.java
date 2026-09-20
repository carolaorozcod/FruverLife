package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Service.GestorUsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String usuario,
                        @RequestParam String contrasena,
                        Model model,
                        HttpSession session) {

        Optional<Usuario> userOpt = gestorUsuarioService.buscarPorUsuario(usuario);

        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();

            if (user.getContrasena().equals(contrasena)
                    && user.getEstado().equals("ACTIVO")){

                session.setAttribute("usuarioLogueado", user);

                if (user.getCargo().equalsIgnoreCase("Gerente")) {
                    return "redirect:/menu/gerente";

                } else if (user.getCargo().equalsIgnoreCase("administrador")) {
                    return "redirect:/menu/administrador";

                } else {

                    return "redirect:/menu/cajero";
                }
            }
        }

        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    @GetMapping("/inicio")
    public String inicio(HttpSession session) {

        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/auth/login";
        }

        return "inicio";
    }
}