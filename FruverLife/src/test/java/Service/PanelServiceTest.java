package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Repositories.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanelServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private ProductoRespository productoRepository;
    @Mock private PersonasRepositories personasRepository;
    @Mock private RegistroDesperdicioService desperdicioService;
    @Mock private Productoservice productoService;

    @InjectMocks private PanelService service;

    // Usamos un mes del pasado para que los resultados no dependan de la fecha en que se corra el test
    private static final YearMonth ENERO = YearMonth.of(2020, 1);

    // ---------- Datos de apoyo ----------
    private Date fecha(int anio, int mes, int dia) {
        return Date.from(LocalDateTime.of(anio, mes, dia, 12, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    private Venta.DetalleVenta linea(String productoId, String nombre, double cantidad, double subtotal) {
        Venta.DetalleVenta d = new Venta.DetalleVenta();
        d.setProductoId(productoId);
        d.setNombre(nombre);
        d.setCantidad(cantidad);
        d.setSubtotal(subtotal);
        return d;
    }

    private Venta venta(Date fecha, Double total, Venta.DetalleVenta... detalle) {
        Venta v = new Venta();
        v.setFecha(fecha);
        v.setTotal(total);
        v.setDetalle(new ArrayList<>(List.of(detalle)));
        return v;
    }

    private Compra compra(Date fecha, Double total) {
        Compra c = new Compra();
        c.setFecha(fecha);
        c.setTotal(total);
        return c;
    }

    private RegistroDesperdicio desperdicio(Date fecha, Double total, String motivo) {
        RegistroDesperdicio r = new RegistroDesperdicio();
        r.setFecha(fecha);
        r.setTotal(total);
        r.setMotivo(motivo);
        return r;
    }

    private Producto producto(String id, String categoria, Double precioCompra) {
        Producto p = new Producto();
        p.setId(id);
        p.setCategoria(categoria);
        p.setPrecioCompra(precioCompra);
        return p;
    }

    // ---------- mesesDisponibles ----------
    @Test
    void mesesDisponibles_empiezaEnElMesActualYRetrocede() {
        Map<String, String> meses = service.mesesDisponibles(3);

        assertEquals(3, meses.size());
        assertEquals(YearMonth.now().toString(), meses.keySet().iterator().next());
        meses.forEach((clave, texto) ->
                assertTrue(texto.endsWith(String.valueOf(YearMonth.parse(clave).getYear()))));
    }

    // ---------- ventasPorDia ----------
    @Test
    void ventasPorDia_enUnMesPasadoTieneUnaEntradaPorCadaDia() {
        when(ventaRepository.findAll()).thenReturn(List.of(
                venta(fecha(2020, 1, 10), 100.0),
                venta(fecha(2020, 1, 10), 50.0),
                venta(fecha(2020, 1, 31), null),     // total nulo cuenta como cero
                venta(fecha(2020, 2, 1), 999.0)));   // de otro mes: se ignora

        Map<String, Double> resultado = service.ventasPorDia(ENERO);

        assertEquals(31, resultado.size());
        assertEquals("01", resultado.keySet().iterator().next());
        assertEquals(150.0, resultado.get("10"));
        assertEquals(0.0, resultado.get("31"));
        assertEquals(0.0, resultado.get("01"));
    }

    // ---------- ventasVsComprasMeses ----------
    @Test
    void ventasVsComprasMeses_sumaPorMesEnLosUltimosMeses() {
        when(ventaRepository.findAll()).thenReturn(List.of(
                venta(fecha(2020, 1, 5), 100.0),
                venta(fecha(2020, 3, 5), 300.0),
                venta(fecha(2019, 12, 1), 999.0)));   // fuera de la ventana
        when(compraRepository.findAll()).thenReturn(List.of(
                compra(fecha(2020, 2, 5), 70.0),
                compra(fecha(2020, 3, 9), 30.0)));

        Map<String, Map<String, Double>> resultado = service.ventasVsComprasMeses(YearMonth.of(2020, 3), 3);

        assertEquals(List.of("01/2020", "02/2020", "03/2020"), new ArrayList<>(resultado.get("ventas").keySet()));
        assertEquals(100.0, resultado.get("ventas").get("01/2020"));
        assertEquals(0.0, resultado.get("ventas").get("02/2020"));
        assertEquals(300.0, resultado.get("ventas").get("03/2020"));
        assertEquals(0.0, resultado.get("compras").get("01/2020"));
        assertEquals(70.0, resultado.get("compras").get("02/2020"));
        assertEquals(30.0, resultado.get("compras").get("03/2020"));
    }

    // ---------- topProductos ----------
    @Test
    void topProductos_ordenaPorCantidadVendidaYRespetaElLimite() {
        when(ventaRepository.findAll()).thenReturn(List.of(
                venta(fecha(2020, 1, 5), 0.0,
                        linea("p1", "Manzana", 6, 0), linea("p2", "Pera", 3, 0)),
                venta(fecha(2020, 1, 20), 0.0,
                        linea("p1", "Manzana", 4, 0), linea("p3", "Mango", 7, 0), linea("p9", null, 50, 0)),
                venta(fecha(2020, 2, 1), 0.0, linea("p2", "Pera", 100, 0))));   // otro mes

        Map<String, Double> resultado = service.topProductos(ENERO, 2);

        assertEquals(List.of("Manzana", "Mango"), new ArrayList<>(resultado.keySet()));
        assertEquals(10.0, resultado.get("Manzana"));
        assertEquals(7.0, resultado.get("Mango"));
    }

    // ---------- ventasPorCategoria ----------
    @Test
    void ventasPorCategoria_sumaPorCategoriaYAgrupaLoDesconocidoComoSinCategoria() {
        when(productoRepository.findAll()).thenReturn(List.of(
                producto("p1", "Frutas", 1.0), producto("p2", "  ", 1.0)));
        when(ventaRepository.findAll()).thenReturn(List.of(
                venta(fecha(2020, 1, 5), 0.0,
                        linea("p1", "Manzana", 1, 100), linea("p2", "Raro", 1, 50), linea("p9", "Borrado", 1, 25)),
                venta(fecha(2020, 2, 5), 0.0, linea("p1", "Manzana", 1, 999))));

        Map<String, Double> resultado = service.ventasPorCategoria(ENERO);

        assertEquals(100.0, resultado.get("Frutas"));
        assertEquals(75.0, resultado.get("Sin categoría"));   // p2 (categoría vacía) + p9 (producto inexistente)
    }

    // ---------- desperdicioPorMotivo ----------
    @Test
    void desperdicioPorMotivo_agrupaPorMotivoSinImportarEspacios() {
        when(desperdicioService.listar()).thenReturn(List.of(
                desperdicio(fecha(2020, 1, 2), 100.0, " Vencido "),
                desperdicio(fecha(2020, 1, 3), 50.0, "Vencido"),
                desperdicio(fecha(2020, 1, 4), 30.0, null),
                desperdicio(fecha(2020, 2, 1), 999.0, "Vencido")));   // otro mes

        Map<String, Double> resultado = service.desperdicioPorMotivo(ENERO);

        assertEquals(150.0, resultado.get("Vencido"));
        assertEquals(30.0, resultado.get("Sin motivo"));
        assertEquals(2, resultado.size());
    }

    // ---------- ultimosMovimientos ----------
    @Test
    void ultimosMovimientos_mezclaVentasComprasYDesperdiciosDelMasRecienteAlMasAntiguo() {
        Venta v = venta(fecha(2020, 1, 10), 100.0);
        v.setCodigo("VEN-1");
        Compra c = compra(fecha(2020, 1, 20), 70.0);
        c.setCodigo("COM-1");
        RegistroDesperdicio d = desperdicio(fecha(2020, 1, 15), 10.0, "Vencido");
        d.setCodigo("DES-1");
        when(ventaRepository.findAll()).thenReturn(List.of(v));
        when(compraRepository.findAll()).thenReturn(List.of(c));
        when(desperdicioService.listar()).thenReturn(List.of(d));

        List<PanelService.Movimiento> movimientos = service.ultimosMovimientos(ENERO, 2);

        assertEquals(2, movimientos.size());   // el límite descarta el más antiguo (la venta)
        assertEquals("Compra", movimientos.get(0).getTipo());
        assertEquals("COM-1", movimientos.get(0).getCodigo());
        assertEquals(70.0, movimientos.get(0).getMonto(), 0.001);
        assertEquals("Desperdicio", movimientos.get(1).getTipo());
    }

    // ---------- kpis ----------
    @Test
    void kpis_calculaVentasComprasUtilidadYFlujoDeCajaDelMes() {
        Venta enero = venta(fecha(2020, 1, 10), 100000.0, linea("p1", "Manzana", 10, 100000));
        Venta febrero = venta(fecha(2020, 2, 1), 50000.0);   // no cuenta para enero
        when(ventaRepository.findAll()).thenReturn(List.of(enero, febrero));
        when(compraRepository.findAll()).thenReturn(List.of(
                compra(fecha(2020, 1, 5), 70000.0), compra(fecha(2020, 2, 5), 5.0)));
        when(desperdicioService.listar()).thenReturn(List.of(desperdicio(fecha(2020, 1, 20), 10000.0, "Vencido")));
        when(productoRepository.findAll()).thenReturn(List.of(producto("p1", "Frutas", 4000.0)));
        when(personasRepository.findByFuncionesContaining("CLIENTE")).thenReturn(List.of(new Persona(), new Persona()));
        Persona activo = new Persona();
        Persona.DatosProveedor datosActivo = new Persona.DatosProveedor();
        datosActivo.setEstado("Activo");
        activo.setDatosProveedor(datosActivo);
        Persona inactivo = new Persona();
        Persona.DatosProveedor datosInactivo = new Persona.DatosProveedor();
        datosInactivo.setEstado("Inactivo");
        inactivo.setDatosProveedor(datosInactivo);
        when(personasRepository.findByFuncionesContaining("PROVEEDOR"))
                .thenReturn(List.of(activo, inactivo, new Persona()));
        when(productoService.listarProductosAgotandose()).thenReturn(List.of(new Producto()));

        Map<String, Object> kpis = service.kpis(ENERO);

        assertEquals(false, kpis.get("esMesActual"));
        assertEquals(0.0, kpis.get("ventasHoy"));
        assertEquals(100000.0, kpis.get("ventasMes"));
        assertEquals(70000.0, kpis.get("comprasMes"));
        assertEquals(10000.0, kpis.get("desperdicioMes"));
        // utilidad = ventas - costo de lo vendido (10 x 4.000) - desperdicio
        assertEquals(50000.0, kpis.get("utilidadMes"));
        // flujo de caja = ventas - compras - desperdicio
        assertEquals(20000.0, kpis.get("flujoCajaMes"));
        assertEquals(2L, kpis.get("clientes"));
        assertEquals(1L, kpis.get("proveedoresActivos"));
        assertEquals(1, kpis.get("stockBajo"));
    }
}