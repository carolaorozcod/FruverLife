package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaControllerTest {

    @Mock private PersonasRepositories clienteRepository;
    @Mock private ProductoRespository productoRepository;
    @Mock private VentaService ventaService;
    @InjectMocks private VentaController controller;

    private MockHttpSession session;
    private Persona usuario;
    private Persona cliente;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        usuario = new Persona();
        usuario.setId("u1");
        cliente = new Persona();
        cliente.setId("c1");
    }

    private Producto manzana(String estado, double stock) {
        Producto p = new Producto();
        p.setId("p1");
        p.setCodigo("FR01");
        p.setNombre("Manzana");
        p.setEstado(estado);
        p.setPrecioVenta(2000.0);
        p.setCantidad(stock);
        return p;
    }

    private Venta.DetalleVenta linea(double cantidad) {
        Venta.DetalleVenta d = new Venta.DetalleVenta();
        d.setProductoId("p1");
        d.setNombre("Manzana");
        d.setPrecioVenta(2000.0);
        d.setCantidad(cantidad);
        d.setSubtotal(2000.0 * cantidad);
        return d;
    }

    @SuppressWarnings("unchecked")
    private List<Venta.DetalleVenta> carrito() {
        return (List<Venta.DetalleVenta>) session.getAttribute("carrito");
    }

    private void assertErrorVenta(String esperado) {
        assertEquals(esperado, session.getAttribute("errorVenta"));
    }

    // ---------- buscarCliente ----------
    @Test
    void buscarCliente_noExiste_guardaElErrorEnLaSesion() {
        when(clienteRepository.findByIdentificacion("123")).thenReturn(Optional.empty());

        String vista = controller.buscarCliente("123", session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("Cliente no encontrado");
        assertNull(session.getAttribute("clienteActual"));
    }

    @Test
    void buscarCliente_existe_loGuardaComoClienteActual() {
        when(clienteRepository.findByIdentificacion("123")).thenReturn(Optional.of(cliente));

        String vista = controller.buscarCliente("123", session);

        assertEquals("redirect:/menu/ventas", vista);
        assertSame(cliente, session.getAttribute("clienteActual"));
        assertEquals("Cliente encontrado correctamente", session.getAttribute("mensajeVenta"));
        assertNull(session.getAttribute("errorVenta"));
    }

    // ---------- agregarProducto ----------
    @Test
    void agregarProducto_noExiste_muestraError() {
        when(productoRepository.findByCodigo("ZZ")).thenReturn(null);

        String vista = controller.agregarProducto("ZZ", 1.0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("Producto no encontrado");
        assertNull(carrito());
    }

    @Test
    void agregarProducto_inactivo_muestraError() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("INACTIVO", 10.0));

        controller.agregarProducto("FR01", 1.0, session);

        assertErrorVenta("El producto Manzana está inactivo");
        assertNull(carrito());
    }

    @Test
    void agregarProducto_cantidadCero_muestraError() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("ACTIVO", 10.0));

        controller.agregarProducto("FR01", 0, session);

        assertErrorVenta("La cantidad debe ser mayor a cero");
        assertNull(carrito());
    }

    @Test
    void agregarProducto_masDelStock_muestraError() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("ACTIVO", 5.0));

        controller.agregarProducto("FR01", 6.0, session);

        assertErrorVenta("Stock insuficiente para Manzana");
        assertNull(carrito());
    }

    @Test
    void agregarProducto_nuevo_loPoneEnElCarritoConSuSubtotal() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("ACTIVO", 10.0));

        String vista = controller.agregarProducto("FR01", 3.0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertEquals(1, carrito().size());
        assertEquals("p1", carrito().get(0).getProductoId());
        assertEquals(3.0, carrito().get(0).getCantidad());
        assertEquals(6000.0, carrito().get(0).getSubtotal());
        assertEquals("Producto agregado al carrito", session.getAttribute("mensajeVenta"));
        assertNull(session.getAttribute("errorVenta"));
    }

    @Test
    void agregarProducto_yaEstaEnElCarrito_sumaLaCantidad() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("ACTIVO", 10.0));
        List<Venta.DetalleVenta> existente = new ArrayList<>(List.of(linea(2.0)));
        session.setAttribute("carrito", existente);

        controller.agregarProducto("FR01", 3.0, session);

        assertEquals(1, carrito().size());               // no crea otra línea
        assertEquals(5.0, carrito().get(0).getCantidad());
        assertEquals(10000.0, carrito().get(0).getSubtotal());
    }

    @Test
    void agregarProducto_laSumaSuperaElStock_muestraError() {
        when(productoRepository.findByCodigo("FR01")).thenReturn(manzana("ACTIVO", 4.0));
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(2.0))));

        controller.agregarProducto("FR01", 3.0, session);   // 2 + 3 > 4

        assertErrorVenta("Stock insuficiente para Manzana");
        assertEquals(2.0, carrito().get(0).getCantidad());  // el carrito no cambia
    }

    // ---------- eliminarProducto ----------
    @Test
    void eliminarProducto_indiceValido_quitaLaLinea() {
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(1.0), linea(2.0))));

        String vista = controller.eliminarProducto(0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertEquals(1, carrito().size());
        assertEquals(2.0, carrito().get(0).getCantidad());
        assertEquals("Producto eliminado del carrito", session.getAttribute("mensajeVenta"));
    }

    @Test
    void eliminarProducto_indiceFueraDeRango_noCambiaNada() {
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(1.0))));

        controller.eliminarProducto(5, session);

        assertEquals(1, carrito().size());
    }

    @Test
    void eliminarProducto_sinCarrito_noFalla() {
        assertEquals("redirect:/menu/ventas", controller.eliminarProducto(0, session));
    }

    // ---------- finalizarVenta ----------
    @Test
    void finalizarVenta_sinUsuario_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.finalizarVenta(0, session));
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_sinCliente_muestraError() {
        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("carrito", new ArrayList<>(List.of(linea(1.0))));

        String vista = controller.finalizarVenta(0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("Debe buscar un cliente antes de finalizar la venta");
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_carritoVacio_muestraError() {
        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("clienteActual", cliente);

        String vista = controller.finalizarVenta(0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("Debe agregar productos al carrito");
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_ok_redirigeALaFacturaYLimpiaLaSesion() {
        List<Venta.DetalleVenta> items = new ArrayList<>(List.of(linea(1.0)));
        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("clienteActual", cliente);
        session.setAttribute("carrito", items);
        Venta guardada = new Venta();
        guardada.setId("v1");
        when(ventaService.finalizarVenta(usuario, cliente, items, 5000)).thenReturn(guardada);

        String vista = controller.finalizarVenta(5000, session);

        assertEquals("redirect:/ventas/factura/v1", vista);
        assertNull(session.getAttribute("carrito"));
        assertNull(session.getAttribute("clienteActual"));
        assertNull(session.getAttribute("errorVenta"));
    }

    @Test
    void finalizarVenta_siElServiceFalla_muestraElErrorYConservaElCarrito() {
        List<Venta.DetalleVenta> items = new ArrayList<>(List.of(linea(1.0)));
        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("clienteActual", cliente);
        session.setAttribute("carrito", items);
        when(ventaService.finalizarVenta(usuario, cliente, items, 0))
                .thenThrow(new RuntimeException("Stock insuficiente de Manzana"));

        String vista = controller.finalizarVenta(0, session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("No se pudo realizar la venta: Stock insuficiente de Manzana");
        assertSame(items, session.getAttribute("carrito"));
    }

    // ---------- verFactura ----------
    @Test
    void verFactura_sinUsuario_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.verFactura("v1", new ConcurrentModel(), session));
        verifyNoInteractions(ventaService);
    }

    @Test
    void verFactura_noExiste_muestraError() {
        session.setAttribute("usuarioLogueado", usuario);
        when(ventaService.buscarPorId("v9")).thenReturn(null);

        String vista = controller.verFactura("v9", new ConcurrentModel(), session);

        assertEquals("redirect:/menu/ventas", vista);
        assertErrorVenta("Factura no encontrada");
    }

    @Test
    void verFactura_existe_cargaVentaClienteYCajero() {
        session.setAttribute("usuarioLogueado", usuario);
        ConcurrentModel model = new ConcurrentModel();
        Venta venta = new Venta();
        venta.setId("v1");
        venta.setClienteId("c1");
        venta.setUsuarioId("u1");
        venta.setDetalle(List.of(linea(1.0)));
        when(ventaService.buscarPorId("v1")).thenReturn(venta);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(clienteRepository.findById("u1")).thenReturn(Optional.of(usuario));

        String vista = controller.verFactura("v1", model, session);

        assertEquals("FacturaVenta", vista);
        assertSame(venta, model.getAttribute("venta"));
        assertEquals(venta.getDetalle(), model.getAttribute("detalles"));
        assertSame(cliente, model.getAttribute("cliente"));
        assertSame(usuario, model.getAttribute("cajero"));
    }
}