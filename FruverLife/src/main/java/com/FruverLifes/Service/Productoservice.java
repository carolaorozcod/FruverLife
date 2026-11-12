package com.FruverLifes.Service;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.ProductoRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Productoservice {

    @Autowired
    private ProductoRespository productoRepository;

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto guardarProducto(Producto producto) {

        if (producto.getCantidad() == null || producto.getCantidad() < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        if (producto.getPrecioVenta() == null || producto.getPrecioVenta() < 0) {
            throw new IllegalArgumentException("El precio de venta no puede ser negativo");
        }
        if (producto.getPrecioCompra() != null && producto.getPrecioCompra() < 0) {
            throw new IllegalArgumentException("El precio de compra no puede ser negativo");
        }
        if (productoRepository.existsByCodigo(producto.getCodigo())) {
            throw new IllegalArgumentException(
                    "Ya existe un producto registrado con el código " + producto.getCodigo());
        }

        producto.setId(null);          // evita que llegue un "" como id
        producto.setEstado("ACTIVO");  // todo producto nuevo comienza activo

        return productoRepository.save(producto);
    }

    public Producto inactivar(String id) {
        Producto producto = buscarPorId(id);
        producto.setEstado("INACTIVO");
        return productoRepository.save(producto);
    }

    public Producto activar(String id) {
        Producto producto = buscarPorId(id);
        producto.setEstado("ACTIVO");
        return productoRepository.save(producto);
    }

    public Producto buscarPorId(String id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public Producto buscarPorcodigo(String codigo) {
        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            throw new RuntimeException("Producto con código " + codigo + " no encontrado");
        }
        return producto;
    }

    public Producto editarProducto(String id, Producto nuevo) {

        Producto existente = buscarPorId(id);

        // Solo valida el código si cambió (String se compara con equals, no con !=)
        if (!nuevo.getCodigo().equals(existente.getCodigo())
                && productoRepository.existsByCodigo(nuevo.getCodigo())) {
            throw new IllegalArgumentException(
                    "Ya existe un producto registrado con el código " + nuevo.getCodigo());
        }

        existente.setCodigo(nuevo.getCodigo());
        existente.setNombre(nuevo.getNombre());
        existente.setCategoria(nuevo.getCategoria());
        existente.setPrecioCompra(nuevo.getPrecioCompra());
        existente.setPrecioVenta(nuevo.getPrecioVenta());
        existente.setCantidad(nuevo.getCantidad());
        existente.setUnidadMedida(nuevo.getUnidadMedida());

        return productoRepository.save(existente);
    }

    public void actualizarStock(String idProducto, Double nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        Producto producto = buscarPorId(idProducto);
        producto.setCantidad(nuevaCantidad);
        productoRepository.save(producto);
    }

    public void descontarStock(String codigo, Double cantidad) {
        Producto producto = buscarPorcodigo(codigo);

        if (producto.getCantidad() < cantidad) {
            throw new RuntimeException("No hay suficiente stock. Disponible: " + producto.getCantidad());
        }

        producto.setCantidad(producto.getCantidad() - cantidad);
        productoRepository.save(producto);
    }

    public void sumarStock(String idProducto, Double cantidadASumar) {
        if (cantidadASumar <= 0) {
            throw new IllegalArgumentException("La cantidad a sumar debe ser mayor a cero");
        }
        Producto producto = buscarPorId(idProducto);
        producto.setCantidad(producto.getCantidad() + cantidadASumar);
        productoRepository.save(producto);
    }

    public boolean existeProducto(String codigo) {
        return productoRepository.existsByCodigo(codigo);
    }

    public List<Producto> listarProductosAgotandose() {
        return productoRepository.findByCantidadLessThanAndEstado(5.0, "ACTIVO");
    }

    public List<Producto> listarIngresosRecientes() {
        return productoRepository.findTop10ByOrderByIdDesc();
    }
}