package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/proveedores")
public class ProveedoresControllers {

    @Autowired
    private ProveedorService proveedorService;

    // LISTAR
    @GetMapping("/listar")
    public List<Proveedor> listar() {
        return proveedorService.listar();
    }

    // GUARDAR
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor p) {
        proveedorService.guardar(p);
        return "Proveedor registrado correctamente";
    }

    // EDITAR
    @PostMapping("/editar/{id}")
    public String editar(@PathVariable int id,
                         @ModelAttribute Proveedor proveedor) {
        proveedorService.editar(id, proveedor);
        return "Proveedor actualizado correctamente";
    }

    // ELIMINAR
    @DeleteMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        proveedorService.eliminar(id);
        return "Proveedor eliminado correctamente";
    }
}