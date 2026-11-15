package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Service.ClienteService;
import com.FruverLifes.Service.GestorUsuarioService;
import com.FruverLifes.Service.InformeProveedoresService;
import com.FruverLifes.Service.InventarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.ProveedorService;
import com.FruverLifes.Service.RegistroDesperdicioService;
import com.FruverLifes.Service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock private Productoservice productoService;
    @Mock private RegistroDesperdicioService registroService;
    @Mock private ClienteService clienteService;
    @Mock private GestorUsuarioService gestorUsuarioService;
    @Mock private VentaService ventaService;
    @Mock private ProveedorService proveedorService;
    @Mock private InventarioService inventarioService;
    @Mock private InformeProveedoresService informeProveedoresService;
    @InjectMocks private MenuController controller;

    private MockHttpSession session;
    private ConcurrentModel model;
    private Persona usuario;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        model = new ConcurrentModel();
        usuario = new Persona();
        usuario.setId("u1");
        usuario.setNombre("Ana");
    }

    private void iniciarSesion() {
        session.setAttribute("usuarioLogueado", usuario);
    }

    private Producto producto(String estado, Double cantidad) {
        Producto p = new Producto();
        p.setEstado(estado);
        p.setCantidad(cantidad);
        return p;
    }

    private Persona proveedor(String estado) {
        Persona p = new Persona();
        Persona.DatosProveedor datos = new Persona.DatosProveedor();
        datos.setEstado(estado);
        p.setDatosProveedor(datos);
        return p;
    }

    // ---------- Menús principales (con sesión) ----------
    @Test
    void menuGerente_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.menuB(session, model));
    }

    @Test
    void menuGerente_conSesion_muestraLaPlantillaDelGerente() {
        iniciarSesion();

        String vista = controller.menuB(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        assertSame(usuario, model.getAttribute("usuario"));
        assertEquals("inicio", model.getAttribute("contenido"));
    }

    @Test
    void menuAdministrador_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.menuA(session, model));
    }

    @Test
    void menuAdministrador_conSesion_muestraLaPlantillaDelAdministrador() {
        iniciarSesion();

        assertEquals("layout/plantillaAdministrador", controller.menuA(session, model));
        assertEquals("inicio", model.getAttribute("contenido"));
    }

    @Test
    void menuCajero_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.menuc(session, model));
    }

    @Test
    void menuCajero_conSesion_muestraLaPlantillaDelCajero() {
        iniciarSesion();

        assertEquals("layout/PlantillaCajero", controller.menuc(session, model));
        assertSame(usuario, model.getAttribute("usuario"));
    }

    // ---------- Informe de desperdicios ----------
    @Test
    void informeDesperdicios_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.informeDesperdicios(session, model));
        verifyNoInteractions(registroService);
    }

    @Test
    void informeDesperdicios_armaElMapaDeResponsablesConsultandoCadaUsuarioUnaVez() {
        iniciarSesion();
        RegistroDesperdicio r1 = new RegistroDesperdicio();
        r1.setUsuarioId("u1");
        RegistroDesperdicio r2 = new RegistroDesperdicio();
        r2.setUsuarioId("u2");
        RegistroDesperdicio r3 = new RegistroDesperdicio();
        r3.setUsuarioId("u1");   // repetido: no debe consultarse otra vez
        when(registroService.listar()).thenReturn(List.of(r1, r2, r3));
        when(registroService.total()).thenReturn(15000.0);
        when(gestorUsuarioService.buscarPorId("u1")).thenReturn(usuario);
        when(gestorUsuarioService.buscarPorId("u2")).thenReturn(null);

        String vista = controller.informeDesperdicios(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        assertEquals("InformeDes", model.getAttribute("contenido"));
        assertEquals(15000.0, model.getAttribute("totalGeneral"));
        Map<?, ?> responsables = (Map<?, ?>) model.getAttribute("responsables");
        assertEquals("Ana", responsables.get("u1"));
        assertEquals("Desconocido", responsables.get("u2"));
        verify(gestorUsuarioService, times(1)).buscarPorId("u1");
    }

    // ---------- Informe de mercancía ----------
    @Test
    void informeMercancia_clasificaAgotadosSinStockYCalculaElValorDelInventario() {
        Producto conStock = producto("ACTIVO", 10.0);
        conStock.setId("p1");
        conStock.setPrecioCompra(2000.0);
        Producto agotado = producto("ACTIVO", 0.0);      // tiene proveedores pero ya no queda nada
        agotado.setId("p2");
        agotado.setPrecioCompra(1000.0);
        Producto sinStock = producto("ACTIVO", null);    // nunca se le ingresó mercancía
        sinStock.setId("p3");
        when(productoService.listarTodos()).thenReturn(List.of(conStock, agotado, sinStock));
        Map<String, List<InventarioService.ProveedorProducto>> proveedores =
                Map.of("p1", List.of(), "p2", List.of());
        when(inventarioService.proveedoresPorProducto()).thenReturn(proveedores);

        String vista = controller.verInformeMercancia(model);

        assertEquals("layout/PlantillaGerente", vista);
        assertEquals("Informeme", model.getAttribute("contenido"));
        assertSame(proveedores, model.getAttribute("proveedoresPorProducto"));
        assertEquals(1L, model.getAttribute("agotados"));      // p2
        assertEquals(1L, model.getAttribute("sinStock"));      // p3
        // 10 x 2000 + 0 x 1000; el producto sin cantidad ni precio no suma
        assertEquals(20000.0, (Double) model.getAttribute("valorInventario"), 0.001);
    }

    // ---------- Informe diario ----------
    @Test
    void informeDiario_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.informeDiario(null, session, model));
        verifyNoInteractions(ventaService);
    }

    @Test
    void informeDiario_sinFecha_usaElDiaDeHoyEnBogota() {
        iniciarSesion();
        when(ventaService.listarVentasDelDia(any(LocalDate.class))).thenReturn(List.of());

        controller.informeDiario(null, session, model);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(ventaService).listarVentasDelDia(captor.capture());
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        assertTrue(Math.abs(ChronoUnit.DAYS.between(captor.getValue(), hoy)) <= 1);
        assertEquals(captor.getValue(), model.getAttribute("fecha"));
    }

    @Test
    void informeDiario_calculaTotalCantidadYNombres() {
        iniciarSesion();
        Venta v = new Venta();
        v.setClienteId("c1");
        v.setUsuarioId("u1");
        v.setTotal(20000.0);
        Venta sinTotal = new Venta();   // una venta sin total no debe romper la suma
        LocalDate dia = LocalDate.of(2026, 10, 5);
        when(ventaService.listarVentasDelDia(dia)).thenReturn(List.of(v, sinTotal));
        Persona cliente = new Persona();
        cliente.setNombre("Cliente Uno");
        when(clienteService.buscarPorId("c1")).thenReturn(cliente);
        when(gestorUsuarioService.buscarPorId("u1")).thenReturn(null);

        String vista = controller.informeDiario(dia, session, model);

        assertEquals("layout/PlantillaGerente", vista);
        assertEquals(dia, model.getAttribute("fecha"));
        assertEquals(20000.0, (Double) model.getAttribute("totalDia"), 0.001);
        assertEquals(2, model.getAttribute("cantidadVentas"));
        assertEquals("Cliente Uno", ((Map<?, ?>) model.getAttribute("clientes")).get("c1"));
        assertEquals("Desconocido", ((Map<?, ?>) model.getAttribute("cajeros")).get("u1"));
    }

    // ---------- Usuarios ----------
    @Test
    void menuUsuarios_preparaUnFormularioActivoYLaListaDeUsuarios() {
        iniciarSesion();
        List<Persona> lista = List.of(usuario);
        when(gestorUsuarioService.listarTodosUsuarios()).thenReturn(lista);

        String vista = controller.menuUsuarios(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        Persona form = (Persona) model.getAttribute("usuarioForm");
        assertEquals("ACTIVO", form.getAccesoSistema().getEstado());
        assertTrue(form.getAccesoSistema().getHabilitado());
        assertEquals(lista, model.getAttribute("usuarios"));
        assertEquals("Registrousuario", model.getAttribute("contenido"));
    }

    // ---------- Administrador: mercancía, gestión, proveedores ----------
    @Test
    void mercancia_ofreceSoloProveedoresActivosYUnaLineaVacia() {
        when(proveedorService.listar()).thenReturn(List.of(
                proveedor("Activo"), proveedor("Inactivo"), new Persona()));   // el último no tiene datos de proveedor
        when(productoService.listarTodos()).thenReturn(List.of());
        Map<String, List<InventarioService.ProveedorProducto>> porProducto = Map.of("p1", List.of());
        when(inventarioService.proveedoresPorProducto()).thenReturn(porProducto);

        String vista = controller.mercancia(model);

        assertEquals("layout/plantillaAdministrador", vista);
        assertEquals("Mercancia", model.getAttribute("contenido"));
        assertEquals(1, ((List<?>) model.getAttribute("proveedores")).size());
        Compra compra = (Compra) model.getAttribute("compra");
        assertEquals(1, compra.getDetalle().size());
        assertSame(porProducto, model.getAttribute("proveedoresPorProducto"));
    }

    @Test
    void gestionStock_cargaLosProductos() {
        List<Producto> lista = List.of(producto("ACTIVO", 1.0));
        when(productoService.listarProductos()).thenReturn(lista);

        String vista = controller.gestionStock(model);

        assertEquals("layout/plantillaAdministrador", vista);
        assertEquals("GestionStock", model.getAttribute("contenido"));
        assertEquals(lista, model.getAttribute("productos"));
    }

    @Test
    void proveedores_muestraLaPantallaDeProveedores() {
        assertEquals("layout/plantillaAdministrador", controller.proveedores(model));
        assertEquals("proveedores", model.getAttribute("contenido"));
    }

    // ---------- Cajero ----------
    @Test
    void menuClientes_cargaLaListaYUnFormularioVacio() {
        iniciarSesion();
        List<Persona> lista = List.of(new Persona());
        when(clienteService.listarClientes()).thenReturn(lista);

        String vista = controller.menuClientes(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        assertEquals(lista, model.getAttribute("clientes"));
        assertNotNull(model.getAttribute("cliente"));
        assertEquals("Cliente", model.getAttribute("contenido"));
    }

    @Test
    void verificarPrecio_muestraLaPantallaDeVerificacion() {
        assertEquals("layout/PlantillaCajero", controller.verificar(session, model));
        assertEquals("VerificarPrecio", model.getAttribute("contenido"));
    }

    @Test
    void catalogo_muestraSoloLosProductosActivos() {
        Producto activo = producto("ACTIVO", 1.0);
        when(productoService.listarTodos()).thenReturn(List.of(activo, producto("INACTIVO", 1.0)));

        String vista = controller.catalogo(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        assertEquals(List.of(activo), model.getAttribute("productos"));
    }

    // ---------- Pantalla de ventas (carrito y puntos) ----------
    private Venta.DetalleVenta linea(double subtotal) {
        Venta.DetalleVenta d = new Venta.DetalleVenta();
        d.setSubtotal(subtotal);
        return d;
    }

    private Persona clienteConPuntos(int puntos) {
        Persona c = new Persona();
        c.setId("c1");
        c.setPuntos(puntos);
        return c;
    }

    @Test
    void ventas_sinClienteNiCarrito_todoEnCero() {
        String vista = controller.ventas(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        assertEquals("Ventas", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("carrito"));
        assertEquals(0.0, model.getAttribute("subtotal"));
        assertEquals(0.0, model.getAttribute("descuentoUsuario"));
        assertEquals(false, model.getAttribute("esUsuarioSistema"));
        assertEquals(0.0, model.getAttribute("total"));
        assertEquals(0, model.getAttribute("puntosDisponibles"));
        assertEquals(0, model.getAttribute("maximoPuntosRedimibles"));
        assertNull(model.getAttribute("cliente"));
    }

    @Test
    void ventas_clienteConPuntosSuficientes_limitaLaRedencionAlTreintaPorCiento() {
        Persona clienteSesion = clienteConPuntos(1);
        Persona clienteActualizado = clienteConPuntos(20000);
        session.setAttribute("clienteActual", clienteSesion);
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(30000.0), linea(20000.0))));   // total 50.000
        when(gestorUsuarioService.buscarPorId("c1")).thenReturn(clienteActualizado);

        controller.ventas(session, model);

        assertSame(clienteActualizado, model.getAttribute("cliente"));
        assertSame(clienteActualizado, session.getAttribute("clienteActual"));   // la sesión se refresca
        assertEquals(50000.0, model.getAttribute("total"));
        assertEquals(20000, model.getAttribute("puntosDisponibles"));
        assertEquals(15000.0, model.getAttribute("maximoDescuento"));      // 30% de 50.000
        assertEquals(15000, model.getAttribute("maximoPuntosRedimibles")); // el menor entre 20.000 y 15.000
    }

    @Test
    void ventas_clienteConPocosPuntos_noPuedeRedimir() {
        Persona cliente = clienteConPuntos(5000);
        session.setAttribute("clienteActual", cliente);
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(100000.0))));
        when(gestorUsuarioService.buscarPorId("c1")).thenReturn(cliente);

        controller.ventas(session, model);

        assertEquals(5000, model.getAttribute("puntosDisponibles"));
        assertEquals(0, model.getAttribute("maximoPuntosRedimibles"));   // menos de 10.000 puntos
    }

    @Test
    void ventas_siNoSeEncuentraElClienteActualizado_conservaElDeLaSesion() {
        Persona clienteSesion = clienteConPuntos(3000);
        session.setAttribute("clienteActual", clienteSesion);
        when(gestorUsuarioService.buscarPorId("c1")).thenReturn(null);

        controller.ventas(session, model);

        assertSame(clienteSesion, model.getAttribute("cliente"));
        assertEquals(3000, model.getAttribute("puntosDisponibles"));
    }

    @Test
    void ventas_clienteQueEsUsuarioDelSistema_aplicaElDescuentoAntesDeCalcularElMaximo() {
        Persona empleado = clienteConPuntos(20000);
        session.setAttribute("clienteActual", empleado);
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(50000.0))));
        when(gestorUsuarioService.buscarPorId("c1")).thenReturn(empleado);
        when(ventaService.esUsuarioDelSistema(empleado)).thenReturn(true);
        when(ventaService.calcularDescuentoUsuario(50000.0)).thenReturn(12500.0);

        controller.ventas(session, model);

        assertEquals(true, model.getAttribute("esUsuarioSistema"));
        assertEquals(50000.0, model.getAttribute("subtotal"));
        assertEquals(12500.0, model.getAttribute("descuentoUsuario"));
        assertEquals(37500.0, model.getAttribute("total"));               // 50.000 - 12.500
        assertEquals(11250.0, model.getAttribute("maximoDescuento"));     // 30% de 37.500
        assertEquals(11250, model.getAttribute("maximoPuntosRedimibles")); // el menor entre 20.000 y 11.250
    }

    // ---------- Informe de proveedores ----------
    @Test
    void informeProveedores_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.informeProveedores(null, null, session, model));
        verifyNoInteractions(informeProveedoresService);
    }

    @Test
    void informeProveedores_conMesValido_cargaElInformeDeEseMes() {
        iniciarSesion();
        InformeProveedoresService.Informe informe =
                new InformeProveedoresService.Informe("2026-10", 0.0, 0, "—", List.of());
        when(informeProveedoresService.generar(YearMonth.of(2026, 10))).thenReturn(informe);

        String vista = controller.informeProveedores("2026-10", null, session, model);

        assertEquals("layout/PlantillaGerente", vista);
        assertEquals("InformeProv", model.getAttribute("contenido"));
        assertEquals("2026-10", model.getAttribute("mes"));
        assertSame(informe, model.getAttribute("informe"));
        assertNull(model.getAttribute("proveedorSel"));
        verify(informeProveedoresService, never()).detalle(any(), any());
    }

    @Test
    void informeProveedores_conMesInvalido_usaElMesActual() {
        iniciarSesion();

        controller.informeProveedores("no-es-un-mes", null, session, model);

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(informeProveedoresService).generar(captor.capture());
        assertEquals(captor.getValue().toString(), model.getAttribute("mes"));
        assertNotEquals("no-es-un-mes", model.getAttribute("mes"));
    }

    @Test
    void informeProveedores_conProveedorSeleccionado_agregaElDetalleDeSusCompras() {
        iniciarSesion();
        List<Compra> compras = List.of(new Compra());
        when(informeProveedoresService.detalle("prov1", YearMonth.of(2026, 10))).thenReturn(compras);

        controller.informeProveedores("2026-10", "prov1", session, model);

        assertEquals("prov1", model.getAttribute("proveedorSel"));
        assertSame(compras, model.getAttribute("comprasProveedor"));
    }
}