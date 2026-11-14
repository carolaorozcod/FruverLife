
package com.FruverLifes.Service;

import com.FruverLifes.Model.MovimientoPuntos;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.MovimientoPuntosRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class PuntosService {

    private static final int PUNTOS_POR_COMPRA = 1000;
    private static final double VALOR_POR_CADA_10000 = 10000.0;
    private static final int MINIMO_REDENCION = 10000;
    private static final double PORCENTAJE_MAXIMO = 0.30;

    @Autowired
    private PersonasRepositories personasRepository;

    @Autowired
    private MovimientoPuntosRepository movimientoRepository;

    public int calcularPuntos(double total) {
        return (int) (total / VALOR_POR_CADA_10000)
                * PUNTOS_POR_COMPRA;
    }

    public double calcularDescuentoMaximo(double total) {
        return total * PORCENTAJE_MAXIMO;
    }

    public boolean puedeRedimir(Persona cliente) {
        return cliente != null
                && cliente.getPuntos() >= MINIMO_REDENCION;
    }

    public List<MovimientoPuntos> historial(String clienteId) {
        return movimientoRepository
                .findByClienteIdOrderByFechaDesc(clienteId);
    }

    public int acumularPuntos(
            Persona cliente, Venta venta, double totalPagado) {

        int puntos = calcularPuntos(totalPagado);

        if (puntos <= 0) {
            return 0;
        }

        cliente.setPuntos(cliente.getPuntos() + puntos);
        personasRepository.save(cliente);

        registrarMovimiento(
                cliente.getId(),
                venta.getId(),
                "ACUMULACION",
                puntos,
                0.0
        );

        return puntos;
    }

    public int redimirPuntos(
            Persona cliente,
            Venta venta,
            int puntosSolicitados,
            double totalCompra) {

        if (puntosSolicitados < 0) {
            throw new RuntimeException("Los puntos no pueden ser negativos");
        }

        if (puntosSolicitados == 0) {
            return 0;
        }

        if (!puedeRedimir(cliente)) {
            throw new RuntimeException(
                    "Se necesitan mínimo 10.000 puntos para redimir");
        }

        if (puntosSolicitados > cliente.getPuntos()) {
            throw new RuntimeException("Puntos insuficientes");
        }

        double descuento = puntosSolicitados;

        if (descuento > calcularDescuentoMaximo(totalCompra)) {
            throw new RuntimeException(
                    "El descuento supera el 30% de la compra");
        }

        if (descuento > totalCompra) {
            throw new RuntimeException(
                    "El descuento no puede superar la compra");
        }

        cliente.setPuntos(cliente.getPuntos() - puntosSolicitados);
        personasRepository.save(cliente);

        registrarMovimiento(
                cliente.getId(),
                venta.getId(),
                "REDENCION",
                puntosSolicitados,
                descuento
        );

        return puntosSolicitados;
    }

    private void registrarMovimiento(
            String clienteId,
            String ventaId,
            String tipo,
            int puntos,
            double descuento) {

        MovimientoPuntos movimiento = new MovimientoPuntos();
        movimiento.setClienteId(clienteId);
        movimiento.setVentaId(ventaId);
        movimiento.setTipo(tipo);
        movimiento.setPuntos(puntos);
        movimiento.setValorDescuento(descuento);
        movimiento.setFecha(new Date());

        movimientoRepository.save(movimiento);
    }
}
