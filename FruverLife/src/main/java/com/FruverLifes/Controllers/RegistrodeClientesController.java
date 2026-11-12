package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/Cliente")
public class RegistrodeClientesController {

    @Autowired
    private ClienteService clienteService;

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Persona cliente, RedirectAttributes redirectAttrs) {

        if (vacio(cliente.getNombre()) || vacio(cliente.getIdentificacion()) || vacio(cliente.getCorreo())) {
            redirectAttrs.addFlashAttribute("mensaje", "Todos los campos obligatorios deben llenarse");
            return "redirect:/menu/clientes";
        }

        try {
            boolean esEdicion = cliente.getId() != null && !cliente.getId().isBlank();

            if (esEdicion) {
                // Se carga la persona existente para no borrar datosProveedor ni accesoSistema
                Persona existente = clienteService.buscarPorId(cliente.getId());
                if (existente == null) {
                    redirectAttrs.addFlashAttribute("mensaje", "Cliente no encontrado");
                    return "redirect:/menu/clientes";
                }
                existente.setNombre(cliente.getNombre());
                existente.setIdentificacion(cliente.getIdentificacion());
                existente.setTelefono(cliente.getTelefono());
                existente.setCorreo(cliente.getCorreo());
                clienteService.guardar(existente);
                redirectAttrs.addFlashAttribute("mensaje", "Cliente actualizado correctamente");
            } else {
                cliente.setId(null); // evita que llegue un "" y Mongo lo tome como id
                clienteService.guardar(cliente);
                redirectAttrs.addFlashAttribute("mensaje", "Cliente registrado correctamente");
            }

        } catch (DuplicateKeyException e) {
            redirectAttrs.addFlashAttribute("mensaje", "Ya existe una persona con esa identificación");
        }

        return "redirect:/menu/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable String id, Model model) {
        Persona cliente = clienteService.buscarPorId(id);
        if (cliente == null) return "redirect:/menu/clientes";

        model.addAttribute("cliente", cliente);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("contenido", "Cliente");
        return "layout/PlantillaCajero";
    }
}