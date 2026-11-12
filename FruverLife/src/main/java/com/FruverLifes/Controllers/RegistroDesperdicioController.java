package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Service.GestorUsuarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.RegistroDesperdicioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RegistroDesperdicioController {

    @Autowired
    private Productoservice productoService;

    @Autowired
    private RegistroDesperdicioService registroService;

    @Autowired
    private GestorUsuarioService gestorUsuarioService;

    @GetMapping("/menu/desperdicios")
    public String abrirVista(HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        cargarVista(model, usuario, null);
        return "layout/plantillaAdministrador";
    }

    // BUSCAR PRODUCTO
    @PostMapping("/desperdicios/buscar")
    public String buscarProducto(@RequestParam String codigo,
                                 HttpSession session,
                                 Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        Producto producto = null;

        try {
            producto = productoService.buscarPorcodigo(codigo.trim());
            model.addAttribute("mensaje", "Producto encontrado correctamente");
        } catch (Exception e) {
            model.addAttribute("error", "Producto no encontrado");
        }

        cargarVista(model, usuario, producto);
        return "layout/plantillaAdministrador";
    }

    // GUARDAR REGISTRO
    @PostMapping("/desperdicios/guardar")
    public String guardarRegistro(@RequestParam("codigo") String codigo,
                                  @RequestParam("cantidad") double cantidad,
                                  @RequestParam(value = "motivo", required = false) String motivo,
                                  HttpSession session,
                                  Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            registroService.guardar(codigo, cantidad, motivo, usuario);
            model.addAttribute("mensaje", "¡Registro guardado con éxito!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        cargarVista(model, usuario, null);
        return "layout/plantillaAdministrador";
    }

    // Datos que siempre necesita la pantalla
    private void cargarVista(Model model, Persona usuario, Producto producto) {

        List<RegistroDesperdicio> registros = registroService.listar();

        // id del usuario -> nombre, para mostrar el responsable
        Map<String, String> responsables = new HashMap<>();
        for (RegistroDesperdicio r : registros) {
            String uid = r.getUsuarioId();
            if (uid != null && !responsables.containsKey(uid)) {
                Persona p = gestorUsuarioService.buscarPorId(uid);
                responsables.put(uid, p != null ? p.getNombre() : "Desconocido");
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("producto", producto);
        model.addAttribute("registros", registros);
        model.addAttribute("responsables", responsables);
        model.addAttribute("totalGeneral", registroService.total());
        model.addAttribute("contenido", "Registrodesperdicios");
    }
}