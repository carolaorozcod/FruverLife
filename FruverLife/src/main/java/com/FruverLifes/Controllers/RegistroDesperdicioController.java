package com.FruverLifes.Controllers;
import com.FruverLifes.Model.*;
import com.FruverLifes.Service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class RegistroDesperdicioController {

    @Autowired
    private Productoservice productoService;

    @Autowired
    private RegistroDesperdicioService registroService;

    @GetMapping("/menu/desperdicios")
    public String abrirVista(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "Registrodesperdicios");

        // 👇 ESTO ES LO QUE TE FALTA
        model.addAttribute("registros", registroService.listar());
        model.addAttribute("totalGeneral", registroService.total());

        return "layout/plantillaAdministrador";
    }
    //BUSCAR PRODUCTO
    @PostMapping("/desperdicios/buscar")
    public String buscarProducto(@RequestParam int codigo,
                                 HttpSession session,
                                 Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        Producto producto = null;

        try {
            producto = productoService.buscarPorcodigo(codigo);
            model.addAttribute("mensaje", "Producto encontrado correctamente");
        } catch (Exception e) {
            model.addAttribute("error", "Producto no encontrado");
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("producto", producto);
        model.addAttribute("registros", registroService.listar());
        model.addAttribute("totalGeneral", registroService.total());
        model.addAttribute("contenido", "Registrodesperdicios");

        return "layout/plantillaAdministrador";
    }
    //GUARDAR REGISTRO


    @PostMapping("/desperdicios/guardar")
    public String guardarRegistro(@RequestParam("codigo") int codigo,
                                  @RequestParam("cantidad") int cantidad,
                                  HttpSession session,
                                  Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            // Llamada al servicio
            registroService.guardar(codigo, cantidad, usuario);
            model.addAttribute("mensaje", "¡Registro guardado con éxito!");
        } catch (Exception e) {
            model.addAttribute("error", "Producto no encontrado o stock insuficiente");
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("registros", registroService.listar());
        model.addAttribute("totalGeneral", registroService.total());
        model.addAttribute("contenido", "Registrodesperdicios");

        return "layout/plantillaAdministrador";
    }

}