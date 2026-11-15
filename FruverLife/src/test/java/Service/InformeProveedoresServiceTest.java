package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformeProveedoresServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private PersonasRepositories personasRepository;
    @InjectMocks private InformeProveedoresService service;

    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

    private Persona proveedor(String id, String nombre, String estado) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre(nombre);
        Persona.DatosProveedor datos = new Persona.DatosProveedor();
        datos.setNit("N-" + id);
        datos.setEstado(estado);
        p.setDatosProveedor(datos);
        return p;
    }

    private Compra compra(String proveedorId, double total, Date fecha) {
        Compra c = new Compra();
        c.setProveedorId(proveedorId);
        c.setTotal(total);
        c.setFecha(fecha);
        return c;
    }

    // ---------- generar ----------
    @Test
    void generar_calculaTotalesPorcentajesYOrdenaPorMontoComprado() {
        when(compraRepository.findByFechaBetween(any(Date.class), any(Date.class))).thenReturn(List.of(
                compra("prov1", 40000.0, new Date(1000L)),
                compra("prov1", 20000.0, new Date(5000L)),
                compra("prov2", 40000.0, new Date(2000L))));
        // el repositorio los devuelve desordenados: el informe debe ordenarlos por monto
        when(personasRepository.findByFuncionesContaining("PROVEEDOR")).thenReturn(List.of(
                proveedor("prov3", "Frutas C", "Inactivo"),
                proveedor("prov2", "Frutas B", "Activo"),
                proveedor("prov1", "Frutas A", "Activo")));

        InformeProveedoresService.Informe informe = service.generar(NOVIEMBRE);

        assertEquals("2026-11", informe.mes());
        assertEquals(100000.0, informe.comprasTotales(), 0.001);
        assertEquals(2, informe.proveedoresActivos());
        assertEquals("Frutas A", informe.proveedorTop());

        List<InformeProveedoresService.FilaProveedor> filas = informe.proveedores();
        assertEquals(3, filas.size());

        InformeProveedoresService.FilaProveedor primera = filas.get(0);
        assertEquals("Frutas A", primera.nombre());
        assertEquals("N-prov1", primera.nit());
        assertEquals("Activo", primera.estado());
        assertEquals(2, primera.numCompras());
        assertEquals(60000.0, primera.totalComprado(), 0.001);
        assertEquals(60.0, primera.porcentaje(), 0.001);
        assertEquals(new Date(5000L), primera.ultimaCompra());   // la fecha más reciente

        assertEquals("Frutas B", filas.get(1).nombre());
        assertEquals(40.0, filas.get(1).porcentaje(), 0.001);

        InformeProveedoresService.FilaProveedor sinCompras = filas.get(2);
        assertEquals("Frutas C", sinCompras.nombre());
        assertEquals(0, sinCompras.numCompras());
        assertEquals(0.0, sinCompras.totalComprado(), 0.001);
        assertEquals(0.0, sinCompras.porcentaje(), 0.001);
        assertNull(sinCompras.ultimaCompra());
    }

    @Test
    void generar_sinComprasEnElMes_noDividePorCeroYElTopEsUnGuion() {
        when(compraRepository.findByFechaBetween(any(Date.class), any(Date.class))).thenReturn(List.of());
        Persona sinDatos = new Persona();   // proveedor sin datos de proveedor
        sinDatos.setId("prov2");
        sinDatos.setNombre("Sin datos");
        when(personasRepository.findByFuncionesContaining("PROVEEDOR")).thenReturn(List.of(
                proveedor("prov1", "Frutas A", "Activo"), sinDatos));

        InformeProveedoresService.Informe informe = service.generar(NOVIEMBRE);

        assertEquals(0.0, informe.comprasTotales(), 0.001);
        assertEquals("—", informe.proveedorTop());
        assertEquals(1, informe.proveedoresActivos());
        informe.proveedores().forEach(f -> assertEquals(0.0, f.porcentaje(), 0.001));
        assertTrue(informe.proveedores().stream().anyMatch(f -> f.nit() == null && f.estado() == null));
    }

    @Test
    void generar_consultaDesdeElInicioDelMesHastaElInicioDelSiguienteEnHoraDeBogota() {
        when(compraRepository.findByFechaBetween(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(personasRepository.findByFuncionesContaining("PROVEEDOR")).thenReturn(List.of());

        service.generar(NOVIEMBRE);

        ArgumentCaptor<Date> inicio = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> fin = ArgumentCaptor.forClass(Date.class);
        verify(compraRepository).findByFechaBetween(inicio.capture(), fin.capture());
        // Bogotá es UTC-5; al inicio se le resta 1 ms porque "Between" excluye los extremos
        assertEquals(Instant.parse("2026-11-01T04:59:59.999Z"), inicio.getValue().toInstant());
        assertEquals(Instant.parse("2026-12-01T05:00:00Z"), fin.getValue().toInstant());
    }

    // ---------- detalle ----------
    @Test
    void detalle_devuelveSoloLasComprasDelProveedorDeLaMasRecienteALaMasAntigua() {
        Compra antigua = compra("prov1", 1.0, new Date(1000L));
        Compra reciente = compra("prov1", 2.0, new Date(3000L));
        Compra deOtro = compra("prov2", 3.0, new Date(2000L));
        Compra sinFecha = compra("prov1", 4.0, null);
        when(compraRepository.findByFechaBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(antigua, deOtro, reciente, sinFecha));

        List<Compra> resultado = service.detalle("prov1", NOVIEMBRE);

        assertEquals(List.of(reciente, antigua, sinFecha), resultado);   // las que no tienen fecha van al final
    }
}
