package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.InventarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @PostMapping("/ingresar-lote")
    public String ingresarMercancia(@ModelAttribute Compra compra,
                                    HttpSession session,
                                    RedirectAttributes redirectAttrs) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            inventarioService.registrarCompra(compra, usuario.getId());
            redirectAttrs.addFlashAttribute("mensaje", "Inventario actualizado con éxito.");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/menu/mercancia";
    }
}