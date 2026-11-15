package com.FruverLifes.Service;

import com.FruverLifes.Model.MovimientoPuntos;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.MovimientoPuntosRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuntosServiceTest {

    @Mock private PersonasRepositories personasRepository;
    @Mock private MovimientoPuntosRepository movimientoRepository;
    @InjectMocks private PuntosService service;

    private Persona cliente(int puntos) {
        Persona c = new Persona();
        c.setId("c1");
        c.setPuntos(puntos);
        return c;
    }

    private Venta venta() {
        Venta v = new Venta();
        v.setId("v1");
        return v;
    }

    // ---------- Cálculos (no usan mocks) ----------
    @Test
    void calcularPuntos_daMilPuntosPorCadaDiezMilPesos() {
        assertEquals(0, service.calcularPuntos(9999.0));
        assertEquals(1000, service.calcularPuntos(10000.0));
        assertEquals(1000, service.calcularPuntos(19999.0));
        assertEquals(2000, service.calcularPuntos(25000.0));
    }

    @Test
    void calcularDescuentoMaximo_esElTreintaPorCiento() {
        assertEquals(30000.0, service.calcularDescuentoMaximo(100000.0), 0.001);
    }

    @Test
    void puedeRedimir_necesitaMinimoDiezMilPuntos() {
        assertFalse(service.puedeRedimir(null));
        assertFalse(service.puedeRedimir(cliente(9999)));
        assertTrue(service.puedeRedimir(cliente(10000)));
    }

    @Test
    void historial_delegaEnElRepositorio() {
        List<MovimientoPuntos> lista = List.of(new MovimientoPuntos());
        when(movimientoRepository.findByClienteIdOrderByFechaDesc("c1")).thenReturn(lista);

        assertEquals(lista, service.historial("c1"));
    }

    // ---------- acumularPuntos ----------
    @Test
    void acumularPuntos_conCompraGrande_sumaPuntosYRegistraElMovimiento() {
        Persona c = cliente(500);

        int ganados = service.acumularPuntos(c, venta(), 25000.0);

        assertEquals(2000, ganados);
        assertEquals(2500, c.getPuntos());
        verify(personasRepository).save(c);

        ArgumentCaptor<MovimientoPuntos> captor = ArgumentCaptor.forClass(MovimientoPuntos.class);
        verify(movimientoRepository).save(captor.capture());
        MovimientoPuntos mov = captor.getValue();
        assertEquals("ACUMULACION", mov.getTipo());
        assertEquals("c1", mov.getClienteId());
        assertEquals("v1", mov.getVentaId());
        assertEquals(2000, mov.getPuntos());
        assertEquals(0.0, mov.getValorDescuento());
        assertNotNull(mov.getFecha());
    }

    @Test
    void acumularPuntos_conCompraPequena_noHaceNada() {
        Persona c = cliente(500);

        int ganados = service.acumularPuntos(c, venta(), 5000.0);

        assertEquals(0, ganados);
        assertEquals(500, c.getPuntos());
        verifyNoInteractions(personasRepository, movimientoRepository);
    }

    // ---------- redimirPuntos ----------
    @Test
    void redimirPuntos_negativos_lanzaError() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.redimirPuntos(cliente(20000), venta(), -1, 50000.0));
        assertEquals("Los puntos no pueden ser negativos", ex.getMessage());
    }

    @Test
    void redimirPuntos_cero_devuelveCeroSinTocarNada() {
        int redimidos = service.redimirPuntos(cliente(20000), venta(), 0, 50000.0);

        assertEquals(0, redimidos);
        verifyNoInteractions(personasRepository, movimientoRepository);
    }

    @Test
    void redimirPuntos_sinElMinimo_lanzaError() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.redimirPuntos(cliente(9000), venta(), 1000, 50000.0));
        assertEquals("Se necesitan mínimo 10.000 puntos para redimir", ex.getMessage());
    }

    @Test
    void redimirPuntos_masDeLosQueTiene_lanzaError() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.redimirPuntos(cliente(12000), venta(), 13000, 100000.0));
        assertEquals("Puntos insuficientes", ex.getMessage());
    }

    @Test
    void redimirPuntos_masDelTreintaPorCiento_lanzaError() {
        // compra de 20.000 -> máximo 6.000 de descuento
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.redimirPuntos(cliente(20000), venta(), 15000, 20000.0));
        assertEquals("El descuento supera el 30% de la compra", ex.getMessage());
        verify(personasRepository, never()).save(any());
    }

    @Test
    void redimirPuntos_valido_restaPuntosYRegistraElMovimiento() {
        Persona c = cliente(20000);

        int redimidos = service.redimirPuntos(c, venta(), 5000, 50000.0);

        assertEquals(5000, redimidos);
        assertEquals(15000, c.getPuntos());
        verify(personasRepository).save(c);

        ArgumentCaptor<MovimientoPuntos> captor = ArgumentCaptor.forClass(MovimientoPuntos.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals("REDENCION", captor.getValue().getTipo());
        assertEquals(5000, captor.getValue().getPuntos());
        assertEquals(5000.0, captor.getValue().getValorDescuento());
    }
}

