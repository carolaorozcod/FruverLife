package com.FruverLifes.Service;
import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepositorio;

    public Proveedor guardar(Proveedor p) {
        return proveedorRepositorio.save(p);
    }

    public List<Proveedor> listar() {
        return proveedorRepositorio.findAll();
    }

    public void eliminar(int id_provedores) {

        if (!proveedorRepositorio.existsById(id_provedores)) {
            throw new RuntimeException("El proveedor no existe.");
        }

        try {
            proveedorRepositorio.deleteById(id_provedores);
            proveedorRepositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el proveedor porque tiene productos asociados. " +
                            "Puedes inactivarlo en su lugar."
            );
        }
    }

    public Proveedor inactivar(int id) {
        Proveedor proveedor = proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        proveedor.setActivo(false);
        return proveedorRepositorio.save(proveedor);
    }

    public Proveedor activar(int id) {
        Proveedor proveedor = proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        proveedor.setActivo(true);
        return proveedorRepositorio.save(proveedor);
    }

    public Proveedor editar(int id, Proveedor datosNuevos) {

        Proveedor proveedor = proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        proveedor.setNit(datosNuevos.getNit());
        proveedor.setNombre(datosNuevos.getNombre());
        proveedor.setNumero(datosNuevos.getNumero());
        proveedor.setCorreo(datosNuevos.getCorreo());

        return proveedorRepositorio.save(proveedor);
    }
    public boolean existePorNit(String nit) {
        return proveedorRepositorio.existsByNit(nit);
    }
}