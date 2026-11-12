package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class InventarioService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProductoRespository productoRepository;

    @Autowired
    private PersonasRepositories personasRepository;

    public Compra registrarCompra(Compra form, String usuarioId) {

        // 1. Validar el proveedor
        if (form.getProveedorId() == null || form.getProveedorId().isBlank()) {
            throw new RuntimeException("Debe seleccionar un proveedor.");
        }

        Persona proveedor = personasRepository.findById(form.getProveedorId())
                .filter(p -> p.getFunciones() != null && p.getFunciones().contains("PROVEEDOR"))
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (proveedor.getDatosProveedor() == null
                || !"ACTIVO".equalsIgnoreCase(proveedor.getDatosProveedor().getEstado())) {
            throw new RuntimeException("El proveedor " + proveedor.getNombre()
                    + " está inactivo. No se puede registrar mercancía.");
        }

        if (form.getDetalle() == null || form.getDetalle().isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto.");
        }

        // 2. Validar cada línea y reconstruirla con datos de la base
        List<Compra.DetalleCompra> detalleFinal = new ArrayList<>();
        List<Producto> productos = new ArrayList<>();
        double total = 0;

        for (Compra.DetalleCompra item : form.getDetalle()) {

            if (item.getProductoId() == null || item.getProductoId().isBlank()) {
                throw new RuntimeException("Debe seleccionar un producto.");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a cero.");
            }
            if (item.getPrecioCompra() == null || item.getPrecioCompra() < 0) {
                throw new RuntimeException("El precio de compra no es válido.");
            }

            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RuntimeException("El producto no existe."));

            if (!"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
                throw new RuntimeException("El producto " + producto.getNombre()
                        + " está inactivo. No se puede registrar mercancía.");
            }

            Compra.DetalleCompra d = new Compra.DetalleCompra();
            d.setProductoId(producto.getId());
            d.setNombre(producto.getNombre());
            d.setPrecioCompra(item.getPrecioCompra());
            d.setCantidad(item.getCantidad());
            d.setSubtotal(item.getPrecioCompra() * item.getCantidad());

            detalleFinal.add(d);
            productos.add(producto);
            total += d.getSubtotal();
        }

        // 3. Armar y guardar la compra
        Compra compra = new Compra();
        compra.setCodigo("COM-" + System.currentTimeMillis());
        compra.setFecha(new Date());
        compra.setUsuarioId(usuarioId);
        compra.setProveedorId(proveedor.getId());
        compra.setDetalle(detalleFinal);
        compra.setTotal(total);

        compraRepository.save(compra);

        // 4. Sumar el stock
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            p.setCantidad(p.getCantidad() + detalleFinal.get(i).getCantidad());
            productoRepository.save(p);
        }

        return compra;
    }
}