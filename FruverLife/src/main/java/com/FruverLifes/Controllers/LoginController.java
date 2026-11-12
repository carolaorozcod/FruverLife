package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Security.JwtService;
import com.FruverLifes.Service.GestorUsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        // Ya filtra por estado ACTIVO
        Optional<Persona> userOpt = gestorUsuarioService.buscarPorUsuarioActivo(usuario);

        if (userOpt.isPresent()) {

            Persona user = userOpt.get();
            Persona.AccesoSistema acceso = user.getAccesoSistema();

            boolean credencialesValidas = acceso != null
                    && Boolean.TRUE.equals(acceso.getHabilitado())
                    && acceso.getContrasenaHash() != null
                    && passwordEncoder.matches(contrasena, acceso.getContrasenaHash());

            if (credencialesValidas) {

                List<String> roles = acceso.getRoles();
                String cargo = (roles == null || roles.isEmpty()) ? "CAJERO" : roles.get(0);

                // Generar JWT
                String token = jwtService.generarToken(acceso.getUsuario(), cargo);

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
                if (tieneRol(roles, "GERENTE")) {
                    return "redirect:/menu/gerente";
                } else if (tieneRol(roles, "ADMINISTRADOR")) {
                    return "redirect:/menu/administrador";
                } else {
                    return "redirect:/menu/cajero";
                }
            }
        }

        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    private boolean tieneRol(List<String> roles, String rol) {
        return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(rol));
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