package com.FruverLifes.Service;

import com.FruverLifes.Model.*;

import com.FruverLifes.Repositories.*;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRespository productoRepository;

    @Transactional
    public Venta finalizarVenta(Usuario usuario, Clientes cliente, List<DetalleVenta> carrito) {

        if (carrito == null || carrito.isEmpty()) {
            throw new RuntimeException("Carrito vacío");
        }

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado("PAGADA");

        double total = 0;

        for (DetalleVenta d : carrito) {
            total += d.getSubtotal();
        }

        venta.setTotal(total);

        for (DetalleVenta d : carrito) {
            Producto producto = d.getProducto();

            if (producto.getCantidad() < d.getCantidad()) {
                throw new RuntimeException("Stock insuficiente de " + producto.getNombre());
            }

            d.setVenta(venta);

            producto.setCantidad(producto.getCantidad() - d.getCantidad());
            productoRepository.save(producto);
        }

        venta.setDetalles(carrito);

        return ventaRepository.save(venta);
    }
    public List<Venta> listarVentasDelDia() {
        LocalDateTime inicio = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1).minusSeconds(1);

        return ventaRepository.findByFechaBetween(inicio, fin);
    }

    public Venta buscarPorId(int id) {
        return ventaRepository.findById(id).orElse(null);
    }
}