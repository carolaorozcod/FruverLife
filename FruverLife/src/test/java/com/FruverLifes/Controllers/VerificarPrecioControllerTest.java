package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Repositories.ProductoRespository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificarPrecioControllerTest {

    @Mock private ProductoRespository productoRepository;
    @Mock private HttpSession session;
    @Mock private Model model;
    @Mock private RedirectAttributes redirectAttrs;

    @InjectMocks
    private VerificarPrecioController controller;

    private static final String LOGIN = "redirect:/auth/login";

    // =====================================================
    //                    ABRIR VISTA (GET)
    // =====================================================

    @Test
    void abrirVista_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, controller.abrirVista(session, model));
        verifyNoInteractions(model);
    }

    @Test
    void abrirVista_conSesion_retornaPlantillaCajero() {
        Usuario usuario = mock(Usuario.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(usuario);

        String vista = controller.abrirVista(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        verify(model).addAttribute("usuario", usuario);
        verify(model).addAttribute("contenido", "VerificarPrecio");
    }

    // =====================================================
    //                  BUSCAR PRODUCTO (POST)
    // =====================================================

    @Test
    void buscarProducto_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals(LOGIN, controller.buscarProducto(101, session, redirectAttrs));
        verifyNoInteractions(productoRepository, redirectAttrs);
    }

    @Test
    void buscarProducto_existente_agregaProductoComoFlashAttribute() {
        Producto producto = mock(Producto.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));
        when(productoRepository.findByCodigo(101)).thenReturn(producto);

        String vista = controller.buscarProducto(101, session, redirectAttrs);

        assertEquals("redirect:/menu/precio", vista);
        verify(redirectAttrs).addFlashAttribute("producto", producto);
        verify(redirectAttrs, never()).addFlashAttribute(eq("error"), any());
    }

    @Test
    void buscarProducto_noExiste_agregaErrorComoFlashAttribute() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));
        when(productoRepository.findByCodigo(999)).thenReturn(null);

        String vista = controller.buscarProducto(999, session, redirectAttrs);

        assertEquals("redirect:/menu/precio", vista);
        verify(redirectAttrs).addFlashAttribute("error", "Producto no encontrado");
        verify(redirectAttrs, never()).addFlashAttribute(eq("producto"), any());
    }
}