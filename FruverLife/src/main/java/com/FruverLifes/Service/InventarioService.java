package com.FruverLifes.Service;

import com.FruverLifes.Model.IngresoDTO;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioService {

    @Autowired
    private ProductoRespository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;


    @Transactional
    public void procesarIngreso(List<IngresoDTO> productos) {

        for (IngresoDTO item : productos) {

            Producto producto = productoRepository.findById(item.getCodigo())
                    .orElseThrow(() -> new RuntimeException("Producto con código " + item.getCodigo() + " no encontrado"));

            int cantidadAnterior = producto.getCantidad();
            int cantidadNueva = item.getCantidad();

            producto.setCantidad(cantidadAnterior + cantidadNueva);
            productoRepository.save(producto);
        }
    }

    @Transactional
    public void procesarUnicoIngreso(IngresoDTO item) {

        Producto producto = productoRepository.findByCodigo(item.getCodigo());

        if (producto == null) {
            throw new RuntimeException("El producto " + item.getCodigo() + " no existe.");
        }

        Proveedor proveedor =
                proveedorRepository.findByNit(
                        item.getNit()
                );
        if (proveedor == null) {
            throw new RuntimeException("Proveedor no encontrado");
        }

        int stockActual = producto.getCantidad();
        int cantidadRecibida = item.getCantidad();

        producto.setCantidad(stockActual + cantidadRecibida);
        producto.setProveedor(proveedor);

        productoRepository.save(producto);
    }
}