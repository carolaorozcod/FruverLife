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

    // LISTAR TODOS
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // GUARDAR
    public Producto guardarProducto(Producto producto) {

        if (producto.getCantidad() < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }

        if (producto.getPrecio() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }

        return productoRepository.save(producto);
    }

    // BUSCAR POR ID
    public Producto buscarPorId(int id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public Producto buscarPorcodigo(int codigo) {
        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            throw new RuntimeException("Producto con código " + codigo + " no encontrado");
        }
        return producto;
    }

    // ELIMINAR
    public void eliminarProducto(int id) {
        productoRepository.deleteById(id);
    }

    // EDITAR (CORREGIDO Y SEGURO)
    public Producto editarProducto(int id, Producto nuevo) {

        Producto existente = buscarPorId(id);

        existente.setCodigo(nuevo.getCodigo());
        existente.setNombre(nuevo.getNombre());
        existente.setPrecio(nuevo.getPrecio());
        existente.setCantidad(nuevo.getCantidad());

        return productoRepository.save(existente);
    }

    // ACTUALIZAR STOCK
    public void actualizarStock(int idProducto, int nuevaCantidad) {

        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }

        Producto producto = buscarPorId(idProducto);
        producto.setCantidad(nuevaCantidad);
        productoRepository.save(producto);
    }

    // DESCONTAR STOCK
    public void descontarStock(int codigo, int cantidad) {
        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            throw new RuntimeException("El producto no existe");
        }

        if (producto.getCantidad() < cantidad) {
            throw new RuntimeException("No hay suficiente stock. Disponible: " + producto.getCantidad());
        }

        producto.setCantidad(producto.getCantidad() - cantidad);
        productoRepository.save(producto);
    }

    // SUMAR STOCK
    public void sumarStock(int idProducto, int cantidadASumar) {
        if (cantidadASumar <= 0) {
            throw new IllegalArgumentException("La cantidad a sumar debe ser mayor a cero");
        }

        Producto producto = buscarPorId(idProducto);
        producto.setCantidad(producto.getCantidad() + cantidadASumar);
        productoRepository.save(producto);
    }

    @Autowired
    private ProductoRespository productoRepositorio;

    public boolean existeProducto(int codigo) {
        return productoRepositorio.existsByCodigo(codigo);
    }

    public List<Producto> listarTodos() {
        return productoRepositorio.findAll();
    }

    public List<Producto> listarProductosAgotandose() {
        return productoRepositorio.findByCantidadLessThan(5);
    }

    public List<Producto> listarIngresosRecientes() {
        return productoRepositorio.findTop10ByOrderByIdProductoDesc();
    }

}