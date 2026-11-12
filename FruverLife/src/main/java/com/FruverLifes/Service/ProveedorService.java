package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProveedorService {

    private static final String PROVEEDOR = "PROVEEDOR";

    @Autowired
    private PersonasRepositories proveedorRepositorio;

    private Persona obtenerProveedor(String id) {
        return proveedorRepositorio.findById(id)
                .filter(p -> p.getFunciones() != null && p.getFunciones().contains(PROVEEDOR))
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }

    public Persona guardar(Persona p) {
        if (p.getDatosProveedor() == null || p.getDatosProveedor().getNit() == null) {
            throw new IllegalArgumentException("El NIT es obligatorio");
        }

        String nit = p.getDatosProveedor().getNit();
        if (proveedorRepositorio.existsByDatosProveedorNit(nit)) {
            throw new IllegalArgumentException("Ya existe un proveedor con el mismo nit : " + nit);
        }

        if (p.getFunciones() == null) {
            p.setFunciones(new ArrayList<>());
        }
        if (!p.getFunciones().contains(PROVEEDOR)) {
            p.getFunciones().add(PROVEEDOR);
        }
        if (p.getDatosProveedor().getEstado() == null) {
            p.getDatosProveedor().setEstado("Activo");
        }

        if (p.getIdentificacion() == null || p.getIdentificacion().isBlank()) {
            p.setIdentificacion(nit);
        }

        return proveedorRepositorio.save(p);
    }

    public List<Persona> listar() {
        return proveedorRepositorio.findByFuncionesContaining(PROVEEDOR);
    }

    public void eliminar(String id) {
        Persona proveedor = obtenerProveedor(id);


        proveedor.getFunciones().remove(PROVEEDOR);
        proveedor.setDatosProveedor(null);

        if (proveedor.getFunciones().isEmpty()) {
            proveedorRepositorio.deleteById(id);
        } else {
            proveedorRepositorio.save(proveedor); // sigue siendo cliente/usuario
        }
    }

    public Persona inactivar(String id) {
        Persona proveedor = obtenerProveedor(id);
        proveedor.getDatosProveedor().setEstado("Inactivo");
        return proveedorRepositorio.save(proveedor);
    }

    public Persona activar(String id) {
        Persona proveedor = obtenerProveedor(id);
        proveedor.getDatosProveedor().setEstado("Activo");
        return proveedorRepositorio.save(proveedor);
    }

    public Persona editar(String id, Persona datosNuevos) {
        Persona proveedor = obtenerProveedor(id);

        String nitNuevo = datosNuevos.getDatosProveedor().getNit();
        String nitActual = proveedor.getDatosProveedor().getNit();

        if (!nitNuevo.equals(nitActual) && proveedorRepositorio.existsByDatosProveedorNit(nitNuevo)) {
            throw new IllegalArgumentException("Ya existe un proveedor con el mismo nit : " + nitNuevo);
        }

        proveedor.getDatosProveedor().setNit(nitNuevo);
        proveedor.setNombre(datosNuevos.getNombre());
        proveedor.setTelefono(datosNuevos.getTelefono());
        proveedor.setCorreo(datosNuevos.getCorreo());

        return proveedorRepositorio.save(proveedor);
    }

    public boolean existePorNit(String nit) {
        return proveedorRepositorio.existsByDatosProveedorNit(nit);
    }
}