package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Service.GestorUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class RegistrodeUsuarioControllers {

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @GetMapping
    public String listarUsuarios() {
        return "redirect:/menu/usuarios";
    }
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute("usuarioForm") Usuario usuario, RedirectAttributes redirectAttrs){

        if (usuario.getUsuario() != null) usuario.setUsuario(usuario.getUsuario().trim());
        if (usuario.getContrasena() != null) usuario.setContrasena(usuario.getContrasena().trim());
        if (usuario.getCargo() != null) usuario.setCargo(usuario.getCargo().trim());

        if (usuario.getUsuario() == null || usuario.getUsuario().isEmpty()
                || usuario.getContrasena() == null || usuario.getContrasena().isEmpty()
                || usuario.getCargo() == null || usuario.getCargo().isEmpty()) {

            redirectAttrs.addFlashAttribute("error", "Todos los campos son obligatorios");
            return "redirect:/menu/usuarios";
        }

        if (usuario.getIdUsuario() == null &&
                gestorUsuarioService.buscarPorUsuarioActivo(usuario.getUsuario()).isPresent()){

            redirectAttrs.addFlashAttribute("error", "El usuario ya existe");
            return "redirect:/menu/usuarios";
        }

        boolean esNuevo = usuario.getIdUsuario() == null;

        gestorUsuarioService.registrarUsuarios(usuario);

        redirectAttrs.addFlashAttribute("mensaje",
                esNuevo ? "Usuario registrado correctamente" : "Usuario editado correctamente");

        return "redirect:/menu/usuarios";
    }
    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable int id, Model model, RedirectAttributes redirectAttrs) {
        Usuario usuario = gestorUsuarioService.buscarPorId(id);

        if (usuario == null) {
            redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/menu/usuarios";
        }

        model.addAttribute("usuarioForm", usuario);
        model.addAttribute("usuarios", gestorUsuarioService.listarUsuarios());
        model.addAttribute("contenido", "Registrousuario");

        return "layout/PlantillaGerente";
    }
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable int id, RedirectAttributes redirectAttrs) {
        gestorUsuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario INACTIVO");
        return "redirect:/menu/usuarios";
    }
}