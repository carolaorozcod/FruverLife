package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Repositories.RegistroDesperdicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroDesperdicioServiceTest {

    @Mock private Productoservice productoService;
    @Mock private RegistroDesperdicioRepository registroRepository;
    @InjectMocks private RegistroDesperdicioService service;

    private Persona usuario() {
        Persona u = new Persona();
        u.setId("u1");
        return u;
    }

    private Producto producto(Double cantidad, Double precioCompra) {
        Producto p = new Producto();
        p.setId("p1");
        p.setCodigo("FR01");
        p.setNombre("Manzana");
        p.setCantidad(cantidad);
        p.setPrecioCompra(precioCompra);
        return p;
    }

    private RegistroDesperdicio registroConTotal(Double total) {
        RegistroDesperdicio r = new RegistroDesperdicio();
        r.setTotal(total);
        return r;
    }

    // ---------- guardar ----------
    @Test
    void guardar_cantidadCeroONegativa_lanzaErrorSinConsultarNada() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardar("FR01", 0, "Vencido", usuario()));
        assertEquals("La cantidad debe ser mayor a cero", ex.getMessage());
        verifyNoInteractions(productoService, registroRepository);
    }

    @Test
    void guardar_sinStockSuficiente_lanzaErrorYNoGuarda() {
        when(productoService.buscarPorcodigo("FR01")).thenReturn(producto(2.0, 2000.0));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.guardar("FR01", 3, "Vencido", usuario()));
        assertEquals("No hay suficiente stock. Disponible: 2.0", ex.getMessage());
        verifyNoInteractions(registroRepository);
        verify(productoService, never()).descontarStock(any(), any());
    }

    @Test
    void guardar_datosValidos_guardaElRegistroYDespuesDescuentaElStock() {
        when(productoService.buscarPorcodigo("FR01")).thenReturn(producto(10.0, 2000.0));
        when(registroRepository.save(any(RegistroDesperdicio.class))).thenAnswer(inv -> inv.getArgument(0));

        // el código llega con espacios: el service lo recorta
        RegistroDesperdicio guardado = service.guardar("  FR01 ", 3, "Vencido", usuario());

        assertTrue(guardado.getCodigo().startsWith("DES-"));
        assertEquals("u1", guardado.getUsuarioId());
        assertEquals("Vencido", guardado.getMotivo());
        assertEquals(6000.0, guardado.getTotal());        // 2000 x 3
        assertEquals(1, guardado.getDetalle().size());
        assertEquals("Manzana", guardado.getDetalle().get(0).getNombre());
        assertEquals(2000.0, guardado.getDetalle().get(0).getCostoUnitario());

        InOrder orden = inOrder(registroRepository, productoService);
        orden.verify(registroRepository).save(any(RegistroDesperdicio.class));
        orden.verify(productoService).descontarStock("FR01", 3.0);
    }

    @Test
    void guardar_productoSinPrecioDeCompra_usaCostoCero() {
        when(productoService.buscarPorcodigo("FR01")).thenReturn(producto(10.0, null));
        when(registroRepository.save(any(RegistroDesperdicio.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroDesperdicio guardado = service.guardar("FR01", 3, "Vencido", usuario());

        assertEquals(0.0, guardado.getTotal());
    }

    @Test
    void guardar_sinMotivo_usaMotivoNulo() {
        when(productoService.buscarPorcodigo("FR01")).thenReturn(producto(10.0, 2000.0));
        when(registroRepository.save(any(RegistroDesperdicio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.guardar("FR01", 1, usuario());   // versión de 3 parámetros

        ArgumentCaptor<RegistroDesperdicio> captor = ArgumentCaptor.forClass(RegistroDesperdicio.class);
        verify(registroRepository).save(captor.capture());
        assertNull(captor.getValue().getMotivo());
    }

    // ---------- listar / total ----------
    @Test
    void listar_pideLosRegistrosOrdenadosPorFecha() {
        List<RegistroDesperdicio> lista = List.of(new RegistroDesperdicio());
        when(registroRepository.findAllByOrderByFechaDesc()).thenReturn(lista);

        assertEquals(lista, service.listar());
    }

    @Test
    void total_sumaLosTotalesIgnorandoLosNulos() {
        when(registroRepository.findAll()).thenReturn(List.of(
                registroConTotal(1000.0), registroConTotal(null), registroConTotal(500.0)));

        assertEquals(1500.0, service.total(), 0.001);
    }
}

