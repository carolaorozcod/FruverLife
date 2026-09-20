package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Clientes;
import com.FruverLifes.Service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/Cliente")
public class RegistrodeClientesController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Clientes cliente, RedirectAttributes redirectAttrs) {

        // Validación de campos obligatorios
        if(cliente.getNombre().isEmpty() || cliente.getNumeroCe().isEmpty() || cliente.getCorreo().isEmpty()) {
            redirectAttrs.addFlashAttribute("mensaje", "Todos los campos obligatorios deben llenarse");
            return "redirect:/menu/clientes";
        }

        clienteService.guardar(cliente); // JPA save() detecta si es insert o update automáticamente


        redirectAttrs.addFlashAttribute("mensaje", "Cliente registrado correctamente");

        return "redirect:/menu/clientes";
    }
    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable int id, Model model) {
        Clientes cliente = clienteService.buscarPorId(id);
        if (cliente == null) return "redirect:/menu/clientes";

        model.addAttribute("cliente", cliente);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("contenido", "Cliente");
        return "layout/PlantillaCajero";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCliente(@PathVariable int id, RedirectAttributes redirectAttrs) {
        clienteService.eliminar(id);
        redirectAttrs.addFlashAttribute("mensaje", "Cliente eliminado correctamente");
        return "redirect:/menu/clientes";
    }

}