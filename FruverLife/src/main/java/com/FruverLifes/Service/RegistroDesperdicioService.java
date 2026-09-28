package com.FruverLifes.Service;

import com.FruverLifes.Model.*;
import com.FruverLifes.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
public class RegistroDesperdicioService {

    @Autowired
    private Productoservice productoService;

    @Autowired
    private RegistroDesperdicioRepository registroRepository;

    public List<RegistroDesperdicio> listar() {
        return registroRepository.findAll();
    }

    public double total() {
        return registroRepository.findAll()
                .stream()
                .mapToDouble(RegistroDesperdicio::getTotal)
                .sum();
    }

    public RegistroDesperdicio guardar(int codigo, int cantidad, Usuario usuario) {

        productoService.descontarStock(codigo, cantidad);

        Producto producto = productoService.buscarPorcodigo(codigo);

        RegistroDesperdicio registro = new RegistroDesperdicio();
        registro.setFecha(LocalDate.now());
        registro.setUsuario(usuario.getUsuario());
        registro.setTotal(producto.getPrecio() * cantidad);

        DetalleDesperdicio detalle = new DetalleDesperdicio();
        detalle.setCodigoProducto(producto.getCodigo());
        detalle.setNombreProducto(producto.getNombre());
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(producto.getPrecio() * cantidad);
        detalle.setRegistro(registro);

        registro.setDetalles(List.of(detalle));

        return registroRepository.save(registro);
    }

}