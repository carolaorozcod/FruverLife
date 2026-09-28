package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> guardar(@ModelAttribute Proveedor p) {
        try {
            proveedorService.guardar(p);
            return ResponseEntity.ok("Proveedor registrado correctamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }

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
    public ResponseEntity<String> eliminar(@PathVariable int id) {
        try {
            proveedorService.eliminar(id);
            return ResponseEntity.ok("Proveedor eliminado correctamente");
        } catch (IllegalStateException e) {
            // Tiene productos asociados: no se puede borrar
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // INACTIVAR
    @PatchMapping("/inactivar/{id}")
    public ResponseEntity<String> inactivar(@PathVariable int id) {
        try {
            proveedorService.inactivar(id);
            return ResponseEntity.ok("Proveedor inactivado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ACTIVAR
    @PatchMapping("/activar/{id}")
    public ResponseEntity<String> activar(@PathVariable int id) {
        try {
            proveedorService.activar(id);
            return ResponseEntity.ok("Proveedor activado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}