package com.FruverLifes.Controllers;
import com.FruverLifes.Model.IngresoDTO;
import com.FruverLifes.Service.InventarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private Productoservice productoService;

    @Autowired
    private ProveedorService proveedorService;

    // PROCESAR INGRESO DE MERCANCÍA
    @PostMapping("/ingresar-lote")
    public String ingresarMercancia(
            @ModelAttribute IngresoDTO ingreso,
            RedirectAttributes redirectAttrs
    ) {

        try {

            // VALIDAR PRODUCTO
            if (!productoService.existeProducto(ingreso.getCodigo())) {

                redirectAttrs.addFlashAttribute(
                        "error",
                        "El producto con código "
                                + ingreso.getCodigo()
                                + " no está registrado."
                );

                return "redirect:/menu/mercancia";
            }

            // VALIDAR PROVEEDOR
            if (!proveedorService.existePorNit(ingreso.getNit())) {

                redirectAttrs.addFlashAttribute(
                        "error",
                        "El proveedor con NIT "
                                + ingreso.getNit()
                                + " no existe."
                );

                return "redirect:/menu/mercancia";
            }

            // ACTUALIZAR INVENTARIO
            inventarioService.procesarUnicoIngreso(ingreso);

            redirectAttrs.addFlashAttribute(
                    "mensaje",
                    "Inventario actualizado con éxito."
            );

            return "redirect:/menu/mercancia";

        } catch (Exception e) {

            redirectAttrs.addFlashAttribute(
                    "error",
                    "Error crítico: " + e.getMessage()
            );

            return "redirect:/menu/mercancia";
        }
    }
    @GetMapping("/mercancia")
    public String mostrarMercancia(org.springframework.ui.Model model) {
        try {
            model.addAttribute("ingresoDTO", new IngresoDTO());

            // Verifica que productoService no sea null y que listarTodos devuelva algo
            var lista = productoService.listarTodos();
            model.addAttribute("productos", lista);


            return "menu/mercancia";
        } catch (Exception e) {
            System.out.println("Error al cargar mercancia: " + e.getMessage());
            return "error";
        }
    }
}