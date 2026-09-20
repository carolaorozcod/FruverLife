package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Repositories.ProductoRespository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/verificar")
public class VerificarPrecioController {

    @Autowired
    private ProductoRespository productoRepository;

    /*
    ABRIR VISTA
    */
    @GetMapping
    public String abrirVista(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "VerificarPrecio");

        return "layout/PlantillaCajero";
    }

    /*
    BUSCAR PRODUCTO
    */
    @PostMapping
    public String buscarProducto(
            @RequestParam int codigo,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            redirectAttrs.addFlashAttribute("error", "Producto no encontrado");
        } else {
            redirectAttrs.addFlashAttribute("producto", producto);
        }

        return "redirect:/menu/precio";
    }
}