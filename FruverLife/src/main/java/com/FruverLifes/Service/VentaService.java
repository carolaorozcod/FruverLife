package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRespository productoRepository;

    public Venta finalizarVenta(Persona usuario, Persona cliente, List<Venta.DetalleVenta> carrito) {

        if (carrito == null || carrito.isEmpty()) {
            throw new RuntimeException("Carrito vacío");
        }

        List<Venta.DetalleVenta> detalleFinal = new ArrayList<>();
        List<Producto> productos = new ArrayList<>();
        double total = 0;

        // 1. Validar y recalcular todo con datos de la base
        for (Venta.DetalleVenta item : carrito) {

            Producto p = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getNombre()));

            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("Cantidad inválida para " + p.getNombre());
            }
            if (p.getCantidad() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente de " + p.getNombre());
            }

            Venta.DetalleVenta d = new Venta.DetalleVenta();
            d.setProductoId(p.getId());
            d.setNombre(p.getNombre());
            d.setPrecioVenta(p.getPrecioVenta());
            d.setCantidad(item.getCantidad());
            d.setSubtotal(p.getPrecioVenta() * item.getCantidad());

            detalleFinal.add(d);
            productos.add(p);
            total += d.getSubtotal();
        }

        // 2. Guardar la venta
        long ahora = System.currentTimeMillis();
        Venta venta = new Venta();
        venta.setCodigo("VEN-" + ahora);
        venta.setFactura("FAC-" + ahora);
        venta.setFecha(new Date());
        venta.setUsuarioId(usuario.getId());
        venta.setClienteId(cliente.getId());
        venta.setDetalle(detalleFinal);
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);

        // 3. Descontar el stock
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            p.setCantidad(p.getCantidad() - detalleFinal.get(i).getCantidad());
            productoRepository.save(p);
        }

        return guardada;
    }

    public List<Venta> listarVentasDelDia() {
        ZoneId zona = ZoneId.systemDefault();
        Date inicio = Date.from(LocalDate.now().atStartOfDay(zona).toInstant());
        Date fin = Date.from(LocalDate.now().plusDays(1).atStartOfDay(zona).toInstant());
        return ventaRepository.findByFechaBetween(inicio, fin);
    }

    public Venta buscarPorId(String id) {
        return ventaRepository.findById(id).orElse(null);
    }
}