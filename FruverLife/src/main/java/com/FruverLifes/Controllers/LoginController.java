package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Service.GestorUsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.FruverLifes.Security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String usuario,
                        @RequestParam String contrasena,
                        Model model,
                        HttpSession session,
                        HttpServletResponse response) {

        Optional<Usuario> userOpt = gestorUsuarioService.buscarPorUsuario(usuario);

        if (userOpt.isPresent()) {

            Usuario user = userOpt.get();

            if (user.getContrasena().equals(contrasena)
                    && user.getEstado().equals("ACTIVO")) {

                // Generar JWT
                String token = jwtService.generarToken(
                        user.getUsuario(),
                        user.getCargo()
                );

                // Guardar usuario para mostrarlo en Thymeleaf
                session.setAttribute("usuarioLogueado", user);

                // Crear cookie con el JWT
                Cookie jwtCookie = new Cookie("JWT", token);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(false); // En localhost usamos false
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(60 * 60); // 1 hora

                response.addCookie(jwtCookie);

                // Redireccionar según el cargo
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
    public String cerrarSesion(HttpSession session,
                               HttpServletResponse response) {

        session.invalidate();

        // Eliminar cookie JWT
        Cookie jwtCookie = new Cookie("JWT", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);

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