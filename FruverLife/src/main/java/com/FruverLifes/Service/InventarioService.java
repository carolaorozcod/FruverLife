package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InventarioService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProductoRespository productoRepository;

    @Autowired
    private PersonasRepositories personasRepository;

    /**
     * Un proveedor que ha suministrado un producto:
     * - precio y fecha: los de su compra más reciente
     * - cantidad: el total que ese proveedor ha ingresado de ese producto
     */
    public record ProveedorProducto(String proveedorId, String proveedor, Double precio, Date fecha, Double cantidad) {}

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

        // 3. Armar y guardar la compra (cada compra conserva su propio proveedor)
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
            double actual = p.getCantidad() != null ? p.getCantidad() : 0;
            p.setCantidad(actual + detalleFinal.get(i).getCantidad());
            p.setPrecioCompra(detalleFinal.get(i).getPrecioCompra());   // último precio de compra
            productoRepository.save(p);
        }

        return compra;
    }

    /**
     * id del producto -> TODOS los proveedores que lo han suministrado.
     * Por cada proveedor: último precio, fecha de su última compra y
     * la cantidad total que ha ingresado de ese producto.
     * Sin repetir proveedores y con el más reciente primero.
     */
    public Map<String, List<ProveedorProducto>> proveedoresPorProducto() {
        Map<String, List<ProveedorProducto>> resultado = new HashMap<>();
        Map<String, String> nombres = new HashMap<>();   // evita repetir consultas

        // Las compras vienen de la más reciente a la más antigua
        for (Compra c : compraRepository.findAllByOrderByFechaDesc()) {
            if (c.getDetalle() == null || c.getProveedorId() == null) continue;

            final String provId = c.getProveedorId();
            String nombre = nombres.computeIfAbsent(provId, id ->
                    personasRepository.findById(id)
                            .map(Persona::getNombre)
                            .orElse("Desconocido"));

            for (Compra.DetalleCompra d : c.getDetalle()) {
                List<ProveedorProducto> lista =
                        resultado.computeIfAbsent(d.getProductoId(), k -> new ArrayList<>());
                double cant = d.getCantidad() == null ? 0 : d.getCantidad();

                int pos = -1;
                for (int i = 0; i < lista.size(); i++) {
                    if (lista.get(i).proveedorId().equals(provId)) { pos = i; break; }
                }

                if (pos == -1) {
                    // Primera vez que aparece = su compra más reciente: de ahí salen precio y fecha
                    lista.add(new ProveedorProducto(provId, nombre, d.getPrecioCompra(), c.getFecha(), cant));
                } else {
                    // Ya estaba: solo se suma la cantidad, precio y fecha se quedan
                    ProveedorProducto a = lista.get(pos);
                    lista.set(pos, new ProveedorProducto(
                            a.proveedorId(), a.proveedor(), a.precio(), a.fecha(), a.cantidad() + cant));
                }
            }
        }
        return resultado;
    }

    /** id del producto -> nombre del proveedor de su compra más reciente. */
    public Map<String, String> proveedorPorProducto() {
        Map<String, String> resultado = new HashMap<>();
        for (Map.Entry<String, List<ProveedorProducto>> e : proveedoresPorProducto().entrySet()) {
            if (!e.getValue().isEmpty()) {
                resultado.put(e.getKey(), e.getValue().get(0).proveedor());
            }
        }
        return resultado;
    }
}