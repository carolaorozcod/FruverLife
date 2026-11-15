package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Repositories.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ProductoRespository productoRepository;
    @Mock private PersonasRepositories personasRepository;
    @Mock private PuntosService puntosService;

    @InjectMocks private VentaService service;

    private Persona usuario;
    private Persona cliente;

    @BeforeEach
    void setUp() {
        usuario = persona("u1", 0);
        cliente = persona("c1", 0);
    }

    // ---------- Datos de apoyo ----------
    private Persona persona(String id, int puntos) {
        Persona p = new Persona();
        p.setId(id);
        p.setPuntos(puntos);
        return p;
    }

    private Producto manzana(double precioVenta, double cantidad) {
        Producto p = new Producto();
        p.setId("p1");
        p.setNombre("Manzana");
        p.setPrecioVenta(precioVenta);
        p.setCantidad(cantidad);
        return p;
    }

    private Venta.DetalleVenta item(Double cantidad) {
        Venta.DetalleVenta d = new Venta.DetalleVenta();
        d.setProductoId("p1");
        d.setCantidad(cantidad);
        return d;
    }

    private void assertError(String esperado, Persona u, Persona c, List<Venta.DetalleVenta> carrito, int puntos) {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.finalizarVenta(u, c, carrito, puntos));
        assertEquals(esperado, ex.getMessage());
    }

    // ---------- Validaciones iniciales ----------
    @Test
    void finalizarVenta_sinUsuario_lanzaError() {
        assertError("Usuario o cliente no válido", null, cliente, List.of(item(1.0)), 0);
    }

    @Test
    void finalizarVenta_sinCliente_lanzaError() {
        assertError("Usuario o cliente no válido", usuario, null, List.of(item(1.0)), 0);
    }

    @Test
    void finalizarVenta_carritoVacioONulo_lanzaError() {
        assertError("Carrito vacío", usuario, cliente, List.of(), 0);
        assertError("Carrito vacío", usuario, cliente, null, 0);
        verifyNoInteractions(personasRepository, ventaRepository);
    }

    @Test
    void finalizarVenta_clienteNoExisteEnLaBase_lanzaError() {
        when(personasRepository.findById("c1")).thenReturn(Optional.empty());

        assertError("Cliente no encontrado", usuario, cliente, List.of(item(1.0)), 0);
    }

    // ---------- Validación del carrito ----------
    @Test
    void finalizarVenta_productoNoExiste_lanzaError() {
        when(personasRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findById("p1")).thenReturn(Optional.empty());

        assertError("Producto no encontrado", usuario, cliente, List.of(item(1.0)), 0);
    }

    @Test
    void finalizarVenta_cantidadCero_lanzaError() {
        when(personasRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));

        assertError("Cantidad inválida para Manzana", usuario, cliente, List.of(item(0.0)), 0);
    }

    @Test
    void finalizarVenta_stockInsuficiente_lanzaErrorYNoGuarda() {
        when(personasRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));

        assertError("Stock insuficiente de Manzana", usuario, cliente, List.of(item(11.0)), 0);
        verifyNoInteractions(ventaRepository);
    }

    // ---------- Validación de puntos ----------
    @Test
    void finalizarVenta_puntosNegativos_lanzaError() {
        when(personasRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));

        assertError("Los puntos no pueden ser negativos", usuario, cliente, List.of(item(1.0)), -5);
    }

    @Test
    void finalizarVenta_puntosSinPoderRedimir_lanzaError() {
        Persona clienteActual = persona("c1", 500);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));
        when(puntosService.puedeRedimir(clienteActual)).thenReturn(false);

        assertError("Necesitas mínimo 10.000 puntos para redimir", usuario, cliente, List.of(item(1.0)), 100);
    }

    @Test
    void finalizarVenta_masPuntosQueLosDisponibles_lanzaError() {
        Persona clienteActual = persona("c1", 12000);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));
        when(puntosService.puedeRedimir(clienteActual)).thenReturn(true);

        assertError("No tienes suficientes puntos", usuario, cliente, List.of(item(1.0)), 13000);
    }

    @Test
    void finalizarVenta_descuentoMayorAlTreintaPorCiento_lanzaError() {
        Persona clienteActual = persona("c1", 20000);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana(10000.0, 10.0)));
        when(puntosService.puedeRedimir(clienteActual)).thenReturn(true);
        when(puntosService.calcularDescuentoMaximo(20000.0)).thenReturn(6000.0);   // 2 x 10.000 = 20.000

        assertError("El descuento no puede superar el 30% de la compra",
                usuario, cliente, List.of(item(2.0)), 15000);
        verifyNoInteractions(ventaRepository);
    }

    // ---------- Ventas exitosas ----------
    @Test
    void finalizarVenta_sinPuntos_guardaLaVentaYDescuentaElInventario() {
        Persona clienteActual = persona("c1", 0);
        Producto producto = manzana(10000.0, 10.0);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntosService.acumularPuntos(eq(clienteActual), any(Venta.class), eq(20000.0))).thenReturn(2000);

        Venta venta = service.finalizarVenta(usuario, cliente, List.of(item(2.0)));   // versión sin puntos

        assertEquals(20000.0, venta.getTotal());
        assertEquals(0.0, venta.getDescuento());
        assertEquals(0, venta.getPuntosUtilizados());
        assertEquals(2000, venta.getPuntosGanados());
        assertEquals("u1", venta.getUsuarioId());
        assertEquals("c1", venta.getClienteId());
        assertTrue(venta.getCodigo().startsWith("VEN-"));
        assertTrue(venta.getFactura().startsWith("FAC-"));
        assertEquals(1, venta.getDetalle().size());
        assertEquals(20000.0, venta.getDetalle().get(0).getSubtotal());

        assertEquals(8.0, producto.getCantidad());               // 10 - 2
        verify(productoRepository).save(producto);
        verify(ventaRepository, times(2)).save(any(Venta.class)); // se guarda y luego se actualizan los puntos
        verify(puntosService, never()).redimirPuntos(any(Persona.class), any(Venta.class), anyInt(), anyDouble());
    }

    @Test
    void finalizarVenta_conPuntos_aplicaElDescuentoYRedimeLosPuntos() {
        Persona clienteActual = persona("c1", 20000);
        Producto producto = manzana(10000.0, 10.0);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));
        when(puntosService.puedeRedimir(clienteActual)).thenReturn(true);
        when(puntosService.calcularDescuentoMaximo(50000.0)).thenReturn(15000.0);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntosService.acumularPuntos(eq(clienteActual), any(Venta.class), eq(47000.0))).thenReturn(4000);

        Venta venta = service.finalizarVenta(usuario, cliente, List.of(item(5.0)), 3000);

        assertEquals(3000.0, venta.getDescuento());
        assertEquals(3000, venta.getPuntosUtilizados());
        assertEquals(47000.0, venta.getTotal());                  // 50.000 - 3.000
        assertEquals(4000, venta.getPuntosGanados());
        assertEquals(5.0, producto.getCantidad());                // 10 - 5
        verify(puntosService).redimirPuntos(eq(clienteActual), any(Venta.class), eq(3000), eq(50000.0));
    }

    // ---------- Consultas ----------
    @Test
    void listarVentasDelDia_consultaElRangoDeHoyHastaManana() {
        when(ventaRepository.findByFechaBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(new Venta()));

        List<Venta> ventas = service.listarVentasDelDia();

        assertEquals(1, ventas.size());
        ArgumentCaptor<Date> inicio = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> fin = ArgumentCaptor.forClass(Date.class);
        verify(ventaRepository).findByFechaBetween(inicio.capture(), fin.capture());

        long horas = (fin.getValue().getTime() - inicio.getValue().getTime()) / 3_600_000;
        assertEquals(24, horas);   // Bogotá no tiene horario de verano: el día dura siempre 24 h
    }

    @Test
    void buscarPorId_existe_devuelveLaVenta() {
        Venta v = new Venta();
        when(ventaRepository.findById("v1")).thenReturn(Optional.of(v));

        assertSame(v, service.buscarPorId("v1"));
    }

    @Test
    void buscarPorId_noExiste_devuelveNull() {
        when(ventaRepository.findById("x")).thenReturn(Optional.empty());

        assertNull(service.buscarPorId("x"));
    }

    // ---------- Fecha concreta ----------
    @Test
    void listarVentasDelDia_conFecha_usaElDiaCompletoEnHoraDeBogota() {
        when(ventaRepository.findByFechaBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(new Venta()));

        List<Venta> ventas = service.listarVentasDelDia(LocalDate.of(2026, 11, 15));

        assertEquals(1, ventas.size());
        ArgumentCaptor<Date> inicio = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> fin = ArgumentCaptor.forClass(Date.class);
        verify(ventaRepository).findByFechaBetween(inicio.capture(), fin.capture());
        // Bogotá es UTC-5: el día empieza a las 05:00 UTC y termina a las 05:00 UTC del siguiente
        assertEquals(Instant.parse("2026-11-15T05:00:00Z"), inicio.getValue().toInstant());
        assertEquals(Instant.parse("2026-11-16T05:00:00Z"), fin.getValue().toInstant());
    }

    // ---------- Descuento del 25% para usuarios del sistema ----------
    private Persona empleado(Boolean habilitado, String estado) {
        Persona p = persona("c1", 0);
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setHabilitado(habilitado);
        acceso.setEstado(estado);
        p.setAccesoSistema(acceso);
        return p;
    }

    @Test
    void esUsuarioDelSistema_soloSiTieneAccesoActivoYNoDeshabilitado() {
        assertFalse(service.esUsuarioDelSistema(null));
        assertFalse(service.esUsuarioDelSistema(persona("c1", 0)));          // sin acceso al sistema
        assertTrue(service.esUsuarioDelSistema(empleado(true, "ACTIVO")));
        assertTrue(service.esUsuarioDelSistema(empleado(null, "activo")));   // habilitado sin definir cuenta como habilitado
        assertFalse(service.esUsuarioDelSistema(empleado(false, "ACTIVO")));
        assertFalse(service.esUsuarioDelSistema(empleado(true, "INACTIVO")));
    }

    @Test
    void calcularDescuentoUsuario_esElVeinticincoPorCientoConDosDecimales() {
        assertEquals(5000.0, service.calcularDescuentoUsuario(20000.0), 0.001);
        assertEquals(0.0, service.calcularDescuentoUsuario(0.0), 0.001);
        assertEquals(250.25, service.calcularDescuentoUsuario(1001.0), 0.001);
        assertEquals(0.25, service.calcularDescuentoUsuario(1.0), 0.001);
    }

    @Test
    void finalizarVenta_clienteUsuarioDelSistema_aplicaElDescuentoYCalculaPuntosSobreElTotal() {
        Persona clienteActual = empleado(true, "ACTIVO");
        Producto producto = manzana(10000.0, 10.0);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntosService.acumularPuntos(eq(clienteActual), any(Venta.class), eq(15000.0))).thenReturn(1000);

        Venta venta = service.finalizarVenta(usuario, cliente, List.of(item(2.0)));   // subtotal 20.000

        assertEquals(5000.0, venta.getDescuentoEmpleado());   // 25% de 20.000
        assertEquals(15000.0, venta.getTotal());
        assertEquals(0.0, venta.getDescuento());              // sin descuento por puntos
        assertEquals(1000, venta.getPuntosGanados());
    }

    @Test
    void finalizarVenta_usuarioDelSistemaConPuntos_elMaximoSeCalculaSobreElValorYaRebajado() {
        Persona clienteActual = empleado(true, "ACTIVO");
        clienteActual.setPuntos(20000);
        Producto producto = manzana(10000.0, 10.0);
        when(personasRepository.findById("c1")).thenReturn(Optional.of(clienteActual));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));
        when(puntosService.puedeRedimir(clienteActual)).thenReturn(true);
        when(puntosService.calcularDescuentoMaximo(37500.0)).thenReturn(11250.0);   // 50.000 - 12.500 de descuento
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntosService.acumularPuntos(eq(clienteActual), any(Venta.class), eq(34500.0))).thenReturn(3000);

        Venta venta = service.finalizarVenta(usuario, cliente, List.of(item(5.0)), 3000);

        assertEquals(12500.0, venta.getDescuentoEmpleado());
        assertEquals(3000.0, venta.getDescuento());
        assertEquals(34500.0, venta.getTotal());              // 50.000 - 12.500 - 3.000
        verify(puntosService).redimirPuntos(eq(clienteActual), any(Venta.class), eq(3000), eq(37500.0));
    }
}
