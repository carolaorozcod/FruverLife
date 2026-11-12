package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.GestorUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class RegistrodeUsuarioControllers {

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    @GetMapping
    public String listarUsuarios() {
        return "redirect:/menu/usuarios";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute("usuarioForm") Persona form,
                                 RedirectAttributes redirectAttrs) {

        Persona.AccesoSistema accesoForm = form.getAccesoSistema();
        if (accesoForm == null) {
            redirectAttrs.addFlashAttribute("error", "Todos los campos son obligatorios");
            return "redirect:/menu/usuarios";
        }

        String usuarioTxt = accesoForm.getUsuario() == null ? null : accesoForm.getUsuario().trim().toLowerCase();
        String contrasena = accesoForm.getContrasenaHash() == null ? null : accesoForm.getContrasenaHash().trim();

        List<String> roles = accesoForm.getRoles() == null ? new ArrayList<>() :
                accesoForm.getRoles().stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toList());

        boolean esNuevo = vacio(form.getId());

        // Validaciones
        if (vacio(form.getNombre()) || vacio(form.getIdentificacion())
                || vacio(usuarioTxt) || roles.isEmpty()
                || (esNuevo && vacio(contrasena))) {
            redirectAttrs.addFlashAttribute("error", "Todos los campos son obligatorios");
            return "redirect:/menu/usuarios";
        }

        // El usuario no puede repetirse (activo o inactivo), salvo que sea la misma persona
        Optional<Persona> existente = gestorUsuarioService.buscarPorUsuario(usuarioTxt);
        if (existente.isPresent() && !existente.get().getId().equals(form.getId())) {
            redirectAttrs.addFlashAttribute("error", "El usuario ya existe");
            return "redirect:/menu/usuarios";
        }

        // Nuevo: persona nueva. Edición: se carga la existente para no perder datos
        Persona persona;
        Persona.AccesoSistema acceso;

        if (esNuevo) {
            persona = new Persona();
            persona.setFunciones(new ArrayList<>());
            acceso = new Persona.AccesoSistema();
            acceso.setEstado("ACTIVO");
            acceso.setHabilitado(true);
            persona.setAccesoSistema(acceso);
        } else {
            persona = gestorUsuarioService.buscarPorId(form.getId());
            if (persona == null) {
                redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/menu/usuarios";
            }
            acceso = persona.getAccesoSistema();
        }

        persona.setNombre(form.getNombre().trim());
        persona.setIdentificacion(form.getIdentificacion().trim());

        acceso.setUsuario(usuarioTxt);
        acceso.setRoles(roles);

        // Contraseña: solo se cambia si el usuario escribió una nueva
        if (!vacio(contrasena)) {
            acceso.setContrasenaHash(passwordEncoder.encode(contrasena));
        }

        if (persona.getFunciones() == null) {
            persona.setFunciones(new ArrayList<>());
        }
        if (!persona.getFunciones().contains("USUARIO")) {
            persona.getFunciones().add("USUARIO");
        }

        try {
            gestorUsuarioService.registrarUsuarios(persona);
        } catch (DuplicateKeyException e) {
            redirectAttrs.addFlashAttribute("error", "Ya existe una persona con esa identificación");
            return "redirect:/menu/usuarios";
        }

        redirectAttrs.addFlashAttribute("mensaje",
                esNuevo ? "Usuario registrado correctamente" : "Usuario editado correctamente");

        return "redirect:/menu/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable String id, Model model, RedirectAttributes redirectAttrs) {
        Persona usuario = gestorUsuarioService.buscarPorId(id);

        if (usuario == null) {
            redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/menu/usuarios";
        }

        model.addAttribute("usuarioForm", usuario);
        model.addAttribute("usuarios", gestorUsuarioService.listarTodosUsuarios());
        model.addAttribute("contenido", "Registrousuario");

        return "layout/PlantillaGerente";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable String id, RedirectAttributes redirectAttrs) {
        gestorUsuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario INACTIVO");
        return "redirect:/menu/usuarios";
    }

    @GetMapping("/activar/{id}")
    public String activarUsuario(@PathVariable String id, RedirectAttributes redirectAttrs) {
        gestorUsuarioService.activarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario ACTIVO");
        return "redirect:/menu/usuarios";
    }
}