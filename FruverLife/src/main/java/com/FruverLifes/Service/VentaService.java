
package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Venta;

import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Repositories.VentaRepository;
import com.FruverLifes.Repositories.PersonasRepositories;

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

    @Autowired
    private PersonasRepositories personasRepository;

    @Autowired
    private PuntosService puntosService;


    // FINALIZAR VENTA CON PUNTOS
    public Venta finalizarVenta(
            Persona usuario,
            Persona cliente,
            List<Venta.DetalleVenta> carrito,
            int puntosSolicitados) {

        if (usuario == null || cliente == null) {
            throw new RuntimeException(
                    "Usuario o cliente no válido");
        }

        if (carrito == null || carrito.isEmpty()) {
            throw new RuntimeException("Carrito vacío");
        }

        // Consultar cliente actualizado desde MongoDB
        Persona clienteActual =
                personasRepository.findById(cliente.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cliente no encontrado"));

        List<Venta.DetalleVenta> detalleFinal =
                new ArrayList<>();

        List<Producto> productos =
                new ArrayList<>();

        double subtotal = 0;

        // 1. VALIDAR PRODUCTOS Y RECALCULAR PRECIOS
        for (Venta.DetalleVenta item : carrito) {

            Producto p =
                    productoRepository.findById(
                                    item.getProductoId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Producto no encontrado"));

            if (item.getCantidad() == null ||
                    item.getCantidad() <= 0) {

                throw new RuntimeException(
                        "Cantidad inválida para "
                                + p.getNombre());
            }

            if (p.getCantidad() < item.getCantidad()) {

                throw new RuntimeException(
                        "Stock insuficiente de "
                                + p.getNombre());
            }

            Venta.DetalleVenta d =
                    new Venta.DetalleVenta();

            d.setProductoId(p.getId());
            d.setNombre(p.getNombre());
            d.setPrecioVenta(p.getPrecioVenta());
            d.setCantidad(item.getCantidad());

            d.setSubtotal(
                    p.getPrecioVenta() * item.getCantidad());

            detalleFinal.add(d);

            productos.add(p);

            subtotal += d.getSubtotal();
        }

        // 2. VALIDAR PUNTOS SOLICITADOS
        if (puntosSolicitados < 0) {

            throw new RuntimeException(
                    "Los puntos no pueden ser negativos");
        }

        if (puntosSolicitados > 0) {

            // Verificar mínimo de puntos
            if (!puntosService.puedeRedimir(clienteActual)) {

                throw new RuntimeException(
                        "Necesitas mínimo 10.000 puntos para redimir");
            }

            // Verificar saldo disponible
            if (puntosSolicitados >
                    clienteActual.getPuntos()) {

                throw new RuntimeException(
                        "No tienes suficientes puntos");
            }

            // Verificar límite del 30%
            double descuentoMaximo =
                    puntosService.calcularDescuentoMaximo(
                            subtotal);

            if (puntosSolicitados > descuentoMaximo) {

                throw new RuntimeException(
                        "El descuento no puede superar el 30% de la compra");
            }
        }

        // 3. CALCULAR DESCUENTO Y TOTAL
        double descuento = puntosSolicitados;

        double totalPagado = subtotal - descuento;

        if (totalPagado < 0) {
            throw new RuntimeException(
                    "El total de la compra no puede ser negativo");
        }

        // 4. CONSTRUIR LA VENTA
        long ahora = System.currentTimeMillis();

        Venta venta = new Venta();

        venta.setCodigo("VEN-" + ahora);
        venta.setFactura("FAC-" + ahora);
        venta.setFecha(new Date());

        venta.setUsuarioId(usuario.getId());
        venta.setClienteId(clienteActual.getId());

        venta.setDetalle(detalleFinal);

        venta.setDescuento(descuento);

        venta.setPuntosUtilizados(puntosSolicitados);

        venta.setTotal(totalPagado);

        // 5. GUARDAR VENTA
        Venta guardada =
                ventaRepository.save(venta);

        // 6. REDIMIR PUNTOS
        if (puntosSolicitados > 0) {

            puntosService.redimirPuntos(
                    clienteActual,
                    guardada,
                    puntosSolicitados,
                    subtotal
            );
        }

        // 7. ACUMULAR PUNTOS SOBRE EL TOTAL PAGADO
        int puntosGanados =
                puntosService.acumularPuntos(
                        clienteActual,
                        guardada,
                        totalPagado
                );

        guardada.setPuntosGanados(puntosGanados);

        ventaRepository.save(guardada);

        // 8. DESCONTAR INVENTARIO
        for (int i = 0; i < productos.size(); i++) {

            Producto p = productos.get(i);

            p.setCantidad(
                    p.getCantidad()
                            - detalleFinal.get(i).getCantidad());

            productoRepository.save(p);
        }

        return guardada;
    }


    // FINALIZAR VENTA SIN REDIMIR PUNTOS
    public Venta finalizarVenta(
            Persona usuario,
            Persona cliente,
            List<Venta.DetalleVenta> carrito) {

        return finalizarVenta(
                usuario,
                cliente,
                carrito,
                0
        );
    }


    // LISTAR VENTAS DEL DIA
    public List<Venta> listarVentasDelDia() {

        ZoneId zona = ZoneId.systemDefault();

        Date inicio = Date.from(
                LocalDate.now()
                        .atStartOfDay(zona)
                        .toInstant());

        Date fin = Date.from(
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay(zona)
                        .toInstant());

        return ventaRepository.findByFechaBetween(
                inicio,
                fin);
    }


    // BUSCAR VENTA POR ID
    public Venta buscarPorId(String id) {

        return ventaRepository.findById(id)
                .orElse(null);
    }
}
