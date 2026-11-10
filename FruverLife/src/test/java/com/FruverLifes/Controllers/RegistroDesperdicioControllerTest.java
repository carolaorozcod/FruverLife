package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.RegistroDesperdicioService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroDesperdicioControllerTest {

    @Mock private Productoservice productoService;
    @Mock private RegistroDesperdicioService registroService;
    @Mock private HttpSession session;
    @Mock private Model model;

    @InjectMocks
    private RegistroDesperdicioController controller;

    private static final String LOGIN = "redirect:/auth/login";
    private static final String VISTA = "layout/plantillaAdministrador";

    // =====================================================
    //                    ABRIR VISTA (GET)
    // =====================================================

    @Test
    void abrirVista_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, controller.abrirVista(session, model));
        verifyNoInteractions(registroService, productoService, model);
    }

    @Test
    void abrirVista_conSesion_cargaRegistrosYTotal() {
        Usuario usuario = mock(Usuario.class);
        List<RegistroDesperdicio> registros = List.of(mock(RegistroDesperdicio.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(registroService.listar()).thenReturn(registros);
        when(registroService.total()).thenReturn(150.0); // ajusta el tipo si total() no devuelve double

        String vista = controller.abrirVista(session, model);

        assertEquals(VISTA, vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "Registrodesperdicios");
        verify(model).addAttribute("registros", registros);
        verify(model).addAttribute("totalGeneral", 150.0);
    }

    // =====================================================
    //                  BUSCAR PRODUCTO (POST)
    // =====================================================

    @Test
    void buscarProducto_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, controller.buscarProducto(101, session, model));
        verifyNoInteractions(productoService, registroService, model);
    }

    @Test
    void buscarProducto_existente_agregaProductoYMensaje() {
        Usuario usuario = mock(Usuario.class);
        Producto producto = mock(Producto.class);
        List<RegistroDesperdicio> registros = List.of(mock(RegistroDesperdicio.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(productoService.buscarPorcodigo(101)).thenReturn(producto);
        when(registroService.listar()).thenReturn(registros);
        when(registroService.total()).thenReturn(80.0);

        String vista = controller.buscarProducto(101, session, model);

        assertEquals(VISTA, vista);
        verify(model).addAttribute("mensaje", "Producto encontrado correctamente");
        verify(model, never()).addAttribute(eq("error"), any());
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("producto", producto);
        verify(model).addAttribute("registros", registros);
        verify(model).addAttribute("totalGeneral", 80.0);
        verify(model).addAttribute("contenido", "Registrodesperdicios");
    }

    @Test
    void buscarProducto_noExiste_agregaErrorYProductoNulo() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(productoService.buscarPorcodigo(999))
                .thenThrow(new RuntimeException("No existe"));

        String vista = controller.buscarProducto(999, session, model);

        assertEquals(VISTA, vista);
        verify(model).addAttribute("error", "Producto no encontrado");
        verify(model, never()).addAttribute(eq("mensaje"), any());
        verify(model).addAttribute(eq("producto"), isNull());
        verify(model).addAttribute("contenido", "Registrodesperdicios");
    }

    // =====================================================
    //                  GUARDAR REGISTRO (POST)
    // =====================================================

    @Test
    void guardarRegistro_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, controller.guardarRegistro(101, 5, session, model));
        verifyNoInteractions(registroService, model);
    }

    @Test
    void guardarRegistro_exitoso_llamaAlServicioYAgregaMensaje() {
        Usuario usuario = mock(Usuario.class);
        List<RegistroDesperdicio> registros = List.of(mock(RegistroDesperdicio.class));
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        when(registroService.listar()).thenReturn(registros);
        when(registroService.total()).thenReturn(200.0);

        String vista = controller.guardarRegistro(101, 5, session, model);

        assertEquals(VISTA, vista);
        verify(registroService).guardar(101, 5, usuario);
        verify(model).addAttribute("mensaje", "¡Registro guardado con éxito!");
        verify(model, never()).addAttribute(eq("error"), any());
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("registros", registros);
        verify(model).addAttribute("totalGeneral", 200.0);
        verify(model).addAttribute("contenido", "Registrodesperdicios");
    }

    @Test
    void guardarRegistro_conExcepcion_agregaErrorPeroSigueCargandoLaVista() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);
        doThrow(new RuntimeException("Stock insuficiente"))
                .when(registroService).guardar(101, 500, usuario);

        String vista = controller.guardarRegistro(101, 500, session, model);

        assertEquals(VISTA, vista);
        verify(model).addAttribute("error", "Producto no encontrado o stock insuficiente");
        verify(model, never()).addAttribute(eq("mensaje"), any());
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "Registrodesperdicios");
    }
}