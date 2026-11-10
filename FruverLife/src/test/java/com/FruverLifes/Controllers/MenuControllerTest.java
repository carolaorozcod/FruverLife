package com.FruverLifes.Controllers;

import com.FruverLifes.Model.*;
import com.FruverLifes.Service.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock private Productoservice productoService;
    @Mock private RegistroDesperdicioService registroService;
    @Mock private ClienteService clienteService;
    @Mock private GestorUsuarioService gestorUsuarioService;
    @Mock private VentaService ventaService;

    @Mock private HttpSession session;
    @Mock private Model model;

    @InjectMocks
    private MenuController menuController;

    private static final String LOGIN = "redirect:/auth/login";

    // =====================================================
    //                       GERENTE
    // =====================================================

    @Test
    void menuGerente_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.menuB(session, model));
        verifyNoInteractions(model);
    }

    @Test
    void menuGerente_conSesion_retornaPlantillaGerente() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);

        String vista = menuController.menuB(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "inicio");
    }

    @Test
    void informeDesperdicios_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.informeDesperdicios(session, model));
        verifyNoInteractions(registroService);
    }

    @Test
    void informeDesperdicios_conSesion_calculaTotalGeneral() {
        Usuario usuario = mock(Usuario.class);
        RegistroDesperdicio r1 = mock(RegistroDesperdicio.class);
        RegistroDesperdicio r2 = mock(RegistroDesperdicio.class);
        when(r1.getTotal()).thenReturn(100.0);
        when(r2.getTotal()).thenReturn(200.0);
        List<RegistroDesperdicio> lista = List.of(r1, r2);

        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(registroService.listar()).thenReturn(lista);

        String vista = menuController.informeDesperdicios(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("registros", lista);
        verify(model).addAttribute("totalGeneral", 300.0);
        verify(model).addAttribute("contenido", "InformeDes");
    }

    @Test
    void verInformeMercancia_calculaTotalStock() {
        Producto p1 = mock(Producto.class);
        Producto p2 = mock(Producto.class);
        when(p1.getCantidad()).thenReturn(10);
        when(p2.getCantidad()).thenReturn(5);
        List<Producto> productos = List.of(p1, p2);
        when(productoService.listarTodos()).thenReturn(productos);

        String vista = menuController.verInformeMercancia(model);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("productos", productos);
        verify(model).addAttribute("totalStock", 15);
        verify(model).addAttribute("contenido", "Informeme");
    }

    @Test
    void informeDiario_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.informeDiario(session, model));
        verifyNoInteractions(ventaService);
    }

    @Test
    void informeDiario_conSesion_calculaTotalYCantidadDeVentas() {
        Venta v1 = mock(Venta.class);
        Venta v2 = mock(Venta.class);
        Venta v3 = mock(Venta.class);
        when(v1.getTotal()).thenReturn(1000.0);
        when(v2.getTotal()).thenReturn(2500.0);
        when(v3.getTotal()).thenReturn(500.0);
        List<Venta> ventas = List.of(v1, v2, v3);

        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));
        when(ventaService.listarVentasDelDia()).thenReturn(ventas);

        String vista = menuController.informeDiario(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("ventas", ventas);
        verify(model).addAttribute("cantidadVentas", 3);
        verify(model).addAttribute("totalDia", 4000.0);
        verify(model).addAttribute("contenido", "InformeDiario");
    }

    @Test
    void menuUsuarios_cargaFormularioYListaDeUsuarios() {
        Usuario logueado = mock(Usuario.class);
        List<Usuario> usuarios = List.of(mock(Usuario.class), mock(Usuario.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(logueado);
        when(gestorUsuarioService.listarUsuarios()).thenReturn(usuarios);

        String vista = menuController.menuUsuarios(session, model);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("usuarioLogueado", logueado);
        verify(model).addAttribute(eq("usuarioForm"), any(Usuario.class));
        verify(model).addAttribute("usuarios", usuarios);
        verify(model).addAttribute("contenido", "Registrousuario");
    }

    // =====================================================
    //                    ADMINISTRADOR
    // =====================================================

    @Test
    void menuAdministrador_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.menuA(session, model));
        verifyNoInteractions(model);
    }

    @Test
    void menuAdministrador_conSesion_retornaPlantillaAdministrador() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));

        String vista = menuController.menuA(session, model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("contenido", "inicio");
    }

    @Test
    void proveedores_retornaPlantillaConContenidoProveedores() {
        String vista = menuController.proveedores(model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("contenido", "proveedores");
    }

    @Test
    void gestionStock_cargaListaDeProductos() {
        List<Producto> productos = List.of(mock(Producto.class));
        when(productoService.listarProductos()).thenReturn(productos);

        String vista = menuController.gestionStock(model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("contenido", "GestionStock");
        verify(model).addAttribute("productos", productos);
    }

    @Test
    void mercancia_enviaDtoYListaDeProductos() {
        List<Producto> productos = List.of(mock(Producto.class));
        when(productoService.listarTodos()).thenReturn(productos);

        String vista = menuController.mercancia(model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("contenido", "Mercancia");
        verify(model).addAttribute(eq("ingresoDTO"), any(IngresoDTO.class));
        verify(model).addAttribute("productos", productos);
    }

    @Test
    void desperdicios_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.desperdicios(session, model));
        verifyNoInteractions(registroService);
    }

    @Test
    void desperdicios_conSesion_cargaListaYTotal() {
        Usuario usuario = mock(Usuario.class);
        List<RegistroDesperdicio> lista = List.of(mock(RegistroDesperdicio.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(registroService.listar()).thenReturn(lista);
        when(registroService.total()).thenReturn(150.0); // ajusta el tipo si total() no devuelve double

        String vista = menuController.desperdicios(session, model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("desperdicios", lista);
        verify(model).addAttribute("total", 150.0);
        verify(model).addAttribute("contenido", "Registrodesperdicios");
    }

    // =====================================================
    //                        CAJERO
    // =====================================================

    @Test
    void menuCajero_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, menuController.menuc(session, model));
        verifyNoInteractions(model);
    }

    @Test
    void menuCajero_conSesion_retornaPlantillaCajero() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);

        String vista = menuController.menuc(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "inicio");
    }

    @Test
    void menuClientes_cargaClientesYFormularioVacio() {
        Usuario usuario = mock(Usuario.class);
        List<Clientes> clientes = List.of(mock(Clientes.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(clienteService.listarClientes()).thenReturn(clientes);

        String vista = menuController.menuClientes(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("clientes", clientes);
        verify(model).addAttribute(eq("cliente"), any(Clientes.class));
        verify(model).addAttribute("contenido", "Cliente");
    }

    @Test
    void ventas_sinCarrito_creaCarritoVacioConTotalCero() {
        Usuario usuario = mock(Usuario.class);
        Clientes cliente = mock(Clientes.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(session.getAttribute("clienteActual")).thenReturn(cliente);
        when(session.getAttribute("carrito")).thenReturn(null);

        String vista = menuController.ventas(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("cliente", cliente);
        verify(model).addAttribute("carrito", new ArrayList<DetalleVenta>());
        verify(model).addAttribute("total", 0.0);
        verify(model).addAttribute("contenido", "Ventas");
    }

    @Test
    void ventas_conCarrito_sumaLosSubtotales() {
        Usuario usuario = mock(Usuario.class);
        Clientes cliente = mock(Clientes.class);
        DetalleVenta d1 = mock(DetalleVenta.class);
        DetalleVenta d2 = mock(DetalleVenta.class);
        when(d1.getSubtotal()).thenReturn(3000.0);
        when(d2.getSubtotal()).thenReturn(4500.0);
        List<DetalleVenta> carrito = new ArrayList<>(List.of(d1, d2));

        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(session.getAttribute("clienteActual")).thenReturn(cliente);
        when(session.getAttribute("carrito")).thenReturn(carrito);

        String vista = menuController.ventas(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("cliente", cliente);
        verify(model).addAttribute("carrito", carrito);
        verify(model).addAttribute("total", 7500.0);
        verify(model).addAttribute("contenido", "Ventas");
    }

    @Test
    void verificarPrecio_retornaPlantillaConContenidoVerificarPrecio() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);

        String vista = menuController.verificar(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "VerificarPrecio");
    }

    @Test
    void catalogo_cargaProductos() {
        Usuario usuario = mock(Usuario.class);
        List<Producto> productos = List.of(mock(Producto.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(productoService.listarTodos()).thenReturn(productos);

        String vista = menuController.catalogo(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("productos", productos);
        verify(model).addAttribute("contenido", "CatalogoProductos");
    }
}