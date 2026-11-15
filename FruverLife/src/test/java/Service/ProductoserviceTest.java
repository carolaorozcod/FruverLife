package com.FruverLifes.Service;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.ProductoRespository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoserviceTest {

    @Mock private ProductoRespository productoRepository;
    @InjectMocks private Productoservice service;

    private Producto producto(String id, String codigo, Double cantidad, String estado) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo(codigo);
        p.setNombre("Manzana");
        p.setCantidad(cantidad);
        p.setPrecioVenta(3000.0);
        p.setPrecioCompra(2000.0);
        p.setEstado(estado);
        return p;
    }

    // ---------- guardarProducto ----------
    @Test
    void guardarProducto_cantidadNula_lanzaError() {
        Producto p = producto(null, "FR01", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarProducto(p));
        assertEquals("La cantidad no puede ser negativa", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void guardarProducto_precioVentaNegativo_lanzaError() {
        Producto p = producto(null, "FR01", 5.0, null);
        p.setPrecioVenta(-1.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarProducto(p));
        assertEquals("El precio de venta no puede ser negativo", ex.getMessage());
    }

    @Test
    void guardarProducto_precioCompraNegativo_lanzaError() {
        Producto p = producto(null, "FR01", 5.0, null);
        p.setPrecioCompra(-1.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarProducto(p));
        assertEquals("El precio de compra no puede ser negativo", ex.getMessage());
    }

    @Test
    void guardarProducto_codigoRepetido_lanzaError() {
        Producto p = producto(null, "FR01", 5.0, null);
        when(productoRepository.existsByCodigo("FR01")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarProducto(p));
        assertEquals("Ya existe un producto registrado con el código FR01", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void guardarProducto_datosValidos_loGuardaActivoYSinId() {
        Producto p = producto("", "FR01", 5.0, null);   // llega con id vacío
        when(productoRepository.existsByCodigo("FR01")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto guardado = service.guardarProducto(p);

        assertNull(guardado.getId());
        assertEquals("ACTIVO", guardado.getEstado());
        verify(productoRepository).save(p);
    }

    // ---------- buscar ----------
    @Test
    void buscarPorId_noExiste_lanzaError() {
        when(productoRepository.findById("x")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.buscarPorId("x"));
        assertEquals("Producto no encontrado", ex.getMessage());
    }

    @Test
    void buscarPorcodigo_noExiste_lanzaError() {
        when(productoRepository.findByCodigo("ZZ")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.buscarPorcodigo("ZZ"));
        assertEquals("Producto con código ZZ no encontrado", ex.getMessage());
    }

    @Test
    void buscarPorcodigo_existe_devuelveElProducto() {
        Producto p = producto("p1", "FR01", 5.0, "ACTIVO");
        when(productoRepository.findByCodigo("FR01")).thenReturn(p);

        assertSame(p, service.buscarPorcodigo("FR01"));
    }

    // ---------- activar / inactivar ----------
    @Test
    void inactivar_cambiaElEstadoAInactivo() {
        Producto p = producto("p1", "FR01", 5.0, "ACTIVO");
        when(productoRepository.findById("p1")).thenReturn(Optional.of(p));
        when(productoRepository.save(p)).thenReturn(p);

        Producto resultado = service.inactivar("p1");

        assertEquals("INACTIVO", resultado.getEstado());
    }

    @Test
    void activar_cambiaElEstadoAActivo() {
        Producto p = producto("p1", "FR01", 5.0, "INACTIVO");
        when(productoRepository.findById("p1")).thenReturn(Optional.of(p));
        when(productoRepository.save(p)).thenReturn(p);

        Producto resultado = service.activar("p1");

        assertEquals("ACTIVO", resultado.getEstado());
    }

    // ---------- editarProducto ----------
    @Test
    void editarProducto_codigoNuevoYaExiste_lanzaError() {
        Producto existente = producto("p1", "FR01", 5.0, "ACTIVO");
        Producto nuevo = producto(null, "FR02", 5.0, null);
        when(productoRepository.findById("p1")).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCodigo("FR02")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.editarProducto("p1", nuevo));
        assertEquals("Ya existe un producto registrado con el código FR02", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void editarProducto_mismoCodigo_actualizaLosCamposSinConsultarDuplicado() {
        Producto existente = producto("p1", "FR01", 5.0, "ACTIVO");
        Producto nuevo = producto(null, "FR01", 8.0, null);
        nuevo.setNombre("Pera");
        nuevo.setPrecioCompra(9999.0);   // editar ya no modifica el precio de compra
        when(productoRepository.findById("p1")).thenReturn(Optional.of(existente));
        when(productoRepository.save(existente)).thenReturn(existente);

        Producto resultado = service.editarProducto("p1", nuevo);

        assertEquals("Pera", resultado.getNombre());
        assertEquals(8.0, resultado.getCantidad());
        assertEquals("ACTIVO", resultado.getEstado());   // el estado no se toca
        assertEquals(2000.0, resultado.getPrecioCompra());   // el precio de compra tampoco
        verify(productoRepository, never()).existsByCodigo(any());
    }

    // ---------- stock ----------
    @Test
    void actualizarStock_negativo_lanzaError() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarStock("p1", -1.0));
        verifyNoInteractions(productoRepository);
    }

    @Test
    void actualizarStock_valido_guardaLaNuevaCantidad() {
        Producto p = producto("p1", "FR01", 5.0, "ACTIVO");
        when(productoRepository.findById("p1")).thenReturn(Optional.of(p));

        service.actualizarStock("p1", 20.0);

        assertEquals(20.0, p.getCantidad());
        verify(productoRepository).save(p);
    }

    @Test
    void descontarStock_sinStockSuficiente_lanzaError() {
        Producto p = producto("p1", "FR01", 3.0, "ACTIVO");
        when(productoRepository.findByCodigo("FR01")).thenReturn(p);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.descontarStock("FR01", 5.0));
        assertEquals("No hay suficiente stock. Disponible: 3.0", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void descontarStock_conStock_restaLaCantidad() {
        Producto p = producto("p1", "FR01", 10.0, "ACTIVO");
        when(productoRepository.findByCodigo("FR01")).thenReturn(p);

        service.descontarStock("FR01", 4.0);

        assertEquals(6.0, p.getCantidad());
        verify(productoRepository).save(p);
    }

    @Test
    void sumarStock_cantidadCeroONegativa_lanzaError() {
        assertThrows(IllegalArgumentException.class, () -> service.sumarStock("p1", 0.0));
        verifyNoInteractions(productoRepository);
    }

    @Test
    void sumarStock_valido_sumaLaCantidad() {
        Producto p = producto("p1", "FR01", 10.0, "ACTIVO");
        when(productoRepository.findById("p1")).thenReturn(Optional.of(p));

        service.sumarStock("p1", 5.0);

        assertEquals(15.0, p.getCantidad());
        verify(productoRepository).save(p);
    }

    // ---------- consultas ----------
    @Test
    void existeProducto_delegaEnElRepositorio() {
        when(productoRepository.existsByCodigo("FR01")).thenReturn(true);

        assertTrue(service.existeProducto("FR01"));
    }

    @Test
    void listarProductosAgotandose_usaLimiteDeCincoYEstadoActivo() {
        List<Producto> agotandose = List.of(producto("p1", "FR01", 2.0, "ACTIVO"));
        when(productoRepository.findByCantidadLessThanAndEstado(5.0, "ACTIVO")).thenReturn(agotandose);

        assertEquals(agotandose, service.listarProductosAgotandose());
    }
}
