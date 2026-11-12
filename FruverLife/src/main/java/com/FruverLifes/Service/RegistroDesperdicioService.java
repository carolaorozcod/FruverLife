package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Repositories.RegistroDesperdicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class RegistroDesperdicioService {

    @Autowired
    private Productoservice productoService;

    @Autowired
    private RegistroDesperdicioRepository registroRepository;

    public List<RegistroDesperdicio> listar() {
        return registroRepository.findAllByOrderByFechaDesc();
    }

    public double total() {
        return registroRepository.findAll()
                .stream()
                .map(RegistroDesperdicio::getTotal)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    // Misma firma que usaba tu controlador
    public RegistroDesperdicio guardar(String codigo, double cantidad, Persona usuario) {
        return guardar(codigo, cantidad, null, usuario);
    }

    public RegistroDesperdicio guardar(String codigo, double cantidad, String motivo, Persona usuario) {

        // 1. Validar antes de guardar nada
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        Producto producto = productoService.buscarPorcodigo(codigo.trim());

        if (producto.getCantidad() < cantidad) {
            throw new RuntimeException("No hay suficiente stock. Disponible: " + producto.getCantidad());
        }

        // 2. El costo sale del precio de compra del producto
        double costo = producto.getPrecioCompra() != null ? producto.getPrecioCompra() : 0.0;

        RegistroDesperdicio.DetalleDesperdicio detalle = new RegistroDesperdicio.DetalleDesperdicio();
        detalle.setProductoId(producto.getId());
        detalle.setNombre(producto.getNombre());
        detalle.setCantidad(cantidad);
        detalle.setCostoUnitario(costo);
        detalle.setSubtotal(costo * cantidad);

        RegistroDesperdicio registro = new RegistroDesperdicio();
        registro.setCodigo("DES-" + System.currentTimeMillis());
        registro.setFecha(new Date());
        registro.setUsuarioId(usuario.getId());
        registro.setMotivo(motivo);
        registro.setDetalle(List.of(detalle));
        registro.setTotal(detalle.getSubtotal());

        // 3. Guardar el registro y después descontar el stock
        RegistroDesperdicio guardado = registroRepository.save(registro);
        productoService.descontarStock(producto.getCodigo(), cantidad);

        return guardado;
    }
}