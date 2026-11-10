package com.FruverLifes.Controllers;

import com.FruverLifes.Model.*;
import com.FruverLifes.Repositories.ClienteRepository;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaControllerTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ProductoRespository productoRepository;
    @Mock private VentaService ventaService;

    @Mock private HttpSession session;
    @Mock private Model model;

    @Captor private ArgumentCaptor<List<DetalleVenta>> carritoCaptor;

    @InjectMocks
    private VentaController ventaController;

    private static final String REDIRECT_VENTAS = "redirect:/menu/ventas";
    private static final String LOGIN = "redirect:/auth/login";

    // Helper: finalizarVenta lee los 3 atributos siempre, hay que stubear los 3
    private void prepararSesion(Usuario usuario, Clientes cliente, List<DetalleVenta> carrito) {
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(session.getAttribute("clienteActual")).thenReturn(cliente);
        when(session.getAttribute("carrito")).thenReturn(carrito);
    }

    // =====================================================
    //                    BUSCAR CLIENTE
    // =====================================================

    @Test
    void buscarCliente_existente_guardaClienteYMensaje() {
        Clientes cliente = mock(Clientes.class);
        when(clienteRepository.findByIdentificacion(123)).thenReturn(cliente);

        String vista = ventaController.buscarCliente(123, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("clienteActual", cliente);
        verify(session).setAttribute("mensajeVenta", "Cliente encontrado correctamente");
        verify(session).removeAttribute("errorVenta");
    }

    @Test
    void buscarCliente_noExiste_guardaErrorYNoGuardaCliente() {
        when(clienteRepository.findByIdentificacion(999)).thenReturn(null);

        String vista = ventaController.buscarCliente(999, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Cliente no encontrado");
        verify(session).removeAttribute("mensajeVenta");
        verify(session, never()).setAttribute(eq("clienteActual"), any());
    }

    // =====================================================
    //                   AGREGAR PRODUCTO
    // =====================================================

    @Test
    void agregarProducto_productoNoExiste_guardaError() {
        when(productoRepository.findByCodigo(500)).thenReturn(null);

        String vista = ventaController.agregarProducto(500, 2, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Producto no encontrado");
        verify(session).removeAttribute("mensajeVenta");
        verify(session, never()).setAttribute(eq("carrito"), any());
    }

    @Test
    void agregarProducto_cantidadCero_guardaError() {
        // No stubeo getCantidad: el controlador valida la cantidad antes de consultarla
        Producto producto = mock(Producto.class);
        when(productoRepository.findByCodigo(101)).thenReturn(producto);

        String vista = ventaController.agregarProducto(101, 0, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "La cantidad debe ser mayor a cero");
        verify(session).removeAttribute("mensajeVenta");
        verify(session, never()).setAttribute(eq("carrito"), any());
    }

    @Test
    void agregarProducto_cantidadNegativa_guardaError() {
        Producto producto = mock(Producto.class);
        when(productoRepository.findByCodigo(101)).thenReturn(producto);

        String vista = ventaController.agregarProducto(101, -3, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "La cantidad debe ser mayor a cero");
    }

    @Test
    void agregarProducto_stockInsuficiente_guardaErrorConNombre() {
        Producto producto = mock(Producto.class);
        when(producto.getCantidad()).thenReturn(5);
        when(producto.getNombre()).thenReturn("Manzana");
        when(productoRepository.findByCodigo(101)).thenReturn(producto);

        String vista = ventaController.agregarProducto(101, 10, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Stock insuficiente para Manzana");
        verify(session).removeAttribute("mensajeVenta");
        verify(session, never()).setAttribute(eq("carrito"), any());
    }

    @Test
    void agregarProducto_carritoNuevo_creaCarritoConDetalleYSubtotal() {
        Producto producto = mock(Producto.class);
        when(producto.getCantidad()).thenReturn(20);
        when(producto.getPrecio()).thenReturn(2500.0);
        when(productoRepository.findByCodigo(101)).thenReturn(producto);
        when(session.getAttribute("carrito")).thenReturn(null);

        String vista = ventaController.agregarProducto(101, 4, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute(eq("carrito"), carritoCaptor.capture());

        List<DetalleVenta> carrito = carritoCaptor.getValue();
        assertEquals(1, carrito.size());
        DetalleVenta detalle = carrito.get(0);
        assertSame(producto, detalle.getProducto());
        assertEquals(4, detalle.getCantidad());
        assertEquals(2500.0, detalle.getPrecio(), 0.001);
        assertEquals(10000.0, detalle.getSubtotal(), 0.001);

        verify(session).setAttribute("mensajeVenta", "Producto agregado al carrito");
        verify(session).removeAttribute("errorVenta");
    }

    @Test
    void agregarProducto_carritoExistente_agregaAlFinal() {
        Producto producto = mock(Producto.class);
        when(producto.getCantidad()).thenReturn(20);
        when(producto.getPrecio()).thenReturn(1000.0);
        when(productoRepository.findByCodigo(102)).thenReturn(producto);

        List<DetalleVenta> carrito = new ArrayList<>();
        carrito.add(mock(DetalleVenta.class));
        when(session.getAttribute("carrito")).thenReturn(carrito);

        ventaController.agregarProducto(102, 2, session);

        assertEquals(2, carrito.size());
        assertEquals(2000.0, carrito.get(1).getSubtotal(), 0.001);
        verify(session).setAttribute("carrito", carrito);
    }

    // =====================================================
    //                  ELIMINAR PRODUCTO
    // =====================================================

    @Test
    void eliminarProducto_indiceValido_quitaDelCarritoYGuardaMensaje() {
        DetalleVenta d1 = mock(DetalleVenta.class);
        DetalleVenta d2 = mock(DetalleVenta.class);
        List<DetalleVenta> carrito = new ArrayList<>(List.of(d1, d2));
        when(session.getAttribute("carrito")).thenReturn(carrito);

        String vista = ventaController.eliminarProducto(0, session);

        assertEquals(REDIRECT_VENTAS, vista);
        assertEquals(1, carrito.size());
        assertSame(d2, carrito.get(0));
        verify(session).setAttribute("mensajeVenta", "Producto eliminado del carrito");
        verify(session).removeAttribute("errorVenta");
        verify(session).setAttribute("carrito", carrito);
    }

    @Test
    void eliminarProducto_indiceFueraDeRango_noModificaElCarrito() {
        List<DetalleVenta> carrito = new ArrayList<>(List.of(mock(DetalleVenta.class)));
        when(session.getAttribute("carrito")).thenReturn(carrito);

        String vista = ventaController.eliminarProducto(5, session);

        assertEquals(REDIRECT_VENTAS, vista);
        assertEquals(1, carrito.size());
        verify(session, never()).setAttribute(eq("mensajeVenta"), any());
    }

    @Test
    void eliminarProducto_indiceNegativo_noModificaElCarrito() {
        List<DetalleVenta> carrito = new ArrayList<>(List.of(mock(DetalleVenta.class)));
        when(session.getAttribute("carrito")).thenReturn(carrito);

        ventaController.eliminarProducto(-1, session);

        assertEquals(1, carrito.size());
        verify(session, never()).setAttribute(eq("mensajeVenta"), any());
    }

    @Test
    void eliminarProducto_carritoNulo_redirigeSinFallar() {
        when(session.getAttribute("carrito")).thenReturn(null);

        String vista = ventaController.eliminarProducto(0, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session, never()).setAttribute(eq("mensajeVenta"), any());
    }

    // =====================================================
    //                   FINALIZAR VENTA
    // =====================================================

    @Test
    void finalizarVenta_sinSesion_redirigeALogin() {
        prepararSesion(null, null, null);

        assertEquals(LOGIN, ventaController.finalizarVenta(session));
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_sinCliente_guardaError() {
        prepararSesion(mock(Usuario.class), null, List.of(mock(DetalleVenta.class)));

        String vista = ventaController.finalizarVenta(session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Debe buscar un cliente antes de finalizar la venta");
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_carritoNulo_guardaError() {
        prepararSesion(mock(Usuario.class), mock(Clientes.class), null);

        String vista = ventaController.finalizarVenta(session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Debe agregar productos al carrito");
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_carritoVacio_guardaError() {
        prepararSesion(mock(Usuario.class), mock(Clientes.class), new ArrayList<>());

        String vista = ventaController.finalizarVenta(session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Debe agregar productos al carrito");
        verifyNoInteractions(ventaService);
    }

    @Test
    void finalizarVenta_exitosa_limpiaSesionYRedirigeALaFactura() {
        Usuario usuario = mock(Usuario.class);
        Clientes cliente = mock(Clientes.class);
        List<DetalleVenta> carrito = List.of(mock(DetalleVenta.class));
        Venta venta = mock(Venta.class);
        when(venta.getIdVenta()).thenReturn(15);
        prepararSesion(usuario, cliente, carrito);
        when(ventaService.finalizarVenta(usuario, cliente, carrito)).thenReturn(venta);

        String vista = ventaController.finalizarVenta(session);

        assertEquals("redirect:/ventas/factura/15", vista);
        verify(session).removeAttribute("carrito");
        verify(session).removeAttribute("clienteActual");
        verify(session).removeAttribute("errorVenta");
    }

    @Test
    void finalizarVenta_conExcepcion_guardaErrorYConservaElCarrito() {
        Usuario usuario = mock(Usuario.class);
        Clientes cliente = mock(Clientes.class);
        List<DetalleVenta> carrito = List.of(mock(DetalleVenta.class));
        prepararSesion(usuario, cliente, carrito);
        when(ventaService.finalizarVenta(usuario, cliente, carrito))
                .thenThrow(new RuntimeException("Stock insuficiente"));

        String vista = ventaController.finalizarVenta(session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "No se pudo realizar la venta: Stock insuficiente");
        verify(session, never()).removeAttribute("carrito");
        verify(session, never()).removeAttribute("clienteActual");
    }

    // =====================================================
    //                     VER FACTURA
    // =====================================================

    @Test
    void verFactura_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, ventaController.verFactura(1, model, session));
        verifyNoInteractions(ventaService, model);
    }

    @Test
    void verFactura_noExiste_guardaErrorYRedirige() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));
        when(ventaService.buscarPorId(99)).thenReturn(null);

        String vista = ventaController.verFactura(99, model, session);

        assertEquals(REDIRECT_VENTAS, vista);
        verify(session).setAttribute("errorVenta", "Factura no encontrada");
        verifyNoInteractions(model);
    }

    @Test
    void verFactura_existente_agregaVentaYDetalles() {
        Venta venta = mock(Venta.class);
        List<DetalleVenta> detalles = List.of(mock(DetalleVenta.class));
        when(venta.getDetalles()).thenReturn(detalles);
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));
        when(ventaService.buscarPorId(7)).thenReturn(venta);

        String vista = ventaController.verFactura(7, model, session);

        assertEquals("FacturaVenta", vista);
        verify(model).addAttribute("venta", venta);
        verify(model).addAttribute("detalles", detalles);
    }
}