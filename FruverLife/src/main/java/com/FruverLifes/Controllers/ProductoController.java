package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Service.Productoservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private Productoservice productoService;

    // GUARDAR PRODUCTO
    @PostMapping("/guardar")
    public String guardarProducto(
            @ModelAttribute Producto producto,
            RedirectAttributes redirectAttrs
    ) {

        try {

            productoService.guardarProducto(producto);

            redirectAttrs.addFlashAttribute(
                    "mensaje",
                    "Producto guardado exitosamente"
            );

        } catch (Exception e) {

            redirectAttrs.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/menu/gestion";
    }

    // MOSTRAR DATOS PARA EDITAR
    @GetMapping("/editar/{id}")
    public String editarProducto(
            @PathVariable int id,
            Model model
    ) {

        model.addAttribute(
                "producto",
                productoService.buscarPorId(id)
        );

        model.addAttribute(
                "productos",
                productoService.listarProductos()
        );

        model.addAttribute(
                "contenido",
                "GestionStock"
        );

        return "layout/plantillaAdministrador";
    }

    @GetMapping("/informe/mercancia")
    public String generarInforme(Model model) {
        // Enviamos las dos listas a la vista
        model.addAttribute("productosAgotados", productoService.listarProductosAgotandose());
        model.addAttribute("productosRecientes", productoService.listarIngresosRecientes());

        return "/menu/informeMer";
    }

    // ACTUALIZAR PRODUCTO
    @PostMapping("/editar/{id}")
    public String actualizarProducto(
            @PathVariable int id,
            @ModelAttribute Producto producto,
            RedirectAttributes redirectAttrs
    ) {

        try {

            productoService.editarProducto(id, producto);

            redirectAttrs.addFlashAttribute(
                    "mensaje",
                    "Producto actualizado exitosamente"
            );

        } catch (Exception e) {

            redirectAttrs.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/menu/gestion";
    }
    // INACTIVAR PRODUCTO
    @GetMapping("/inactivar/{id}")
    public String inactivarProducto(
            @PathVariable int id,
            RedirectAttributes redirectAttrs
    ) {
        try {
            productoService.inactivar(id);
            redirectAttrs.addFlashAttribute("mensaje", "Producto inactivado correctamente");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/menu/gestion";
    }

    // ACTIVAR PRODUCTO
    @GetMapping("/activar/{id}")
    public String activarProducto(
            @PathVariable int id,
            RedirectAttributes redirectAttrs
    ) {
        try {
            productoService.activar(id);
            redirectAttrs.addFlashAttribute("mensaje", "Producto activado correctamente");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/menu/gestion";
    }

}