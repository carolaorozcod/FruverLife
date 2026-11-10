package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Service.Productoservice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock private Productoservice productoService;
    @Mock private RedirectAttributes redirectAttrs;
    @Mock private Model model;

    @InjectMocks
    private ProductoController productoController;

    private static final String REDIRECT_GESTION = "redirect:/menu/gestion";

    // =====================================================
    //                      GUARDAR
    // =====================================================

    @Test
    void guardarProducto_exitoso_agregaMensajeYRedirige() {
        Producto producto = mock(Producto.class);

        String vista = productoController.guardarProducto(producto, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(productoService).guardarProducto(producto);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Producto guardado exitosamente");
        verify(redirectAttrs, never()).addFlashAttribute(eq("error"), any());
    }

    @Test
    void guardarProducto_conExcepcion_agregaErrorYRedirige() {
        Producto producto = mock(Producto.class);
        doThrow(new RuntimeException("El producto ya existe"))
                .when(productoService).guardarProducto(producto);

        String vista = productoController.guardarProducto(producto, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(redirectAttrs).addFlashAttribute("error", "El producto ya existe");
        verify(redirectAttrs, never()).addFlashAttribute(eq("mensaje"), any());
    }

    // =====================================================
    //                       EDITAR (GET)
    // =====================================================

    @Test
    void editarProducto_cargaProductoListaYContenido() {
        Producto producto = mock(Producto.class);
        List<Producto> productos = List.of(mock(Producto.class), mock(Producto.class));
        when(productoService.buscarPorId(5)).thenReturn(producto);
        when(productoService.listarProductos()).thenReturn(productos);

        String vista = productoController.editarProducto(5, model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(model).addAttribute("producto", producto);
        verify(model).addAttribute("productos", productos);
        verify(model).addAttribute("contenido", "GestionStock");
    }

    // =====================================================
    //                       INFORME
    // =====================================================

    @Test
    void generarInforme_enviaListasDeAgotadosYRecientes() {
        List<Producto> agotados = List.of(mock(Producto.class));
        List<Producto> recientes = List.of(mock(Producto.class), mock(Producto.class));
        when(productoService.listarProductosAgotandose()).thenReturn(agotados);
        when(productoService.listarIngresosRecientes()).thenReturn(recientes);

        String vista = productoController.generarInforme(model);

        assertEquals("/menu/informeMer", vista);
        verify(model).addAttribute("productosAgotados", agotados);
        verify(model).addAttribute("productosRecientes", recientes);
    }

    // =====================================================
    //                    ACTUALIZAR (POST)
    // =====================================================

    @Test
    void actualizarProducto_exitoso_agregaMensajeYRedirige() {
        Producto producto = mock(Producto.class);

        String vista = productoController.actualizarProducto(3, producto, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(productoService).editarProducto(3, producto);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Producto actualizado exitosamente");
        verify(redirectAttrs, never()).addFlashAttribute(eq("error"), any());
    }

    @Test
    void actualizarProducto_conExcepcion_agregaErrorYRedirige() {
        Producto producto = mock(Producto.class);
        doThrow(new RuntimeException("Producto no encontrado"))
                .when(productoService).editarProducto(99, producto);

        String vista = productoController.actualizarProducto(99, producto, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(redirectAttrs).addFlashAttribute("error", "Producto no encontrado");
        verify(redirectAttrs, never()).addFlashAttribute(eq("mensaje"), any());
    }

    // =====================================================
    //                      INACTIVAR
    // =====================================================

    @Test
    void inactivarProducto_exitoso_agregaMensajeYRedirige() {
        String vista = productoController.inactivarProducto(7, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(productoService).inactivar(7);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Producto inactivado correctamente");
    }

    @Test
    void inactivarProducto_conExcepcion_agregaErrorYRedirige() {
        doThrow(new RuntimeException("No se puede inactivar"))
                .when(productoService).inactivar(7);

        String vista = productoController.inactivarProducto(7, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(redirectAttrs).addFlashAttribute("error", "No se puede inactivar");
        verify(redirectAttrs, never()).addFlashAttribute(eq("mensaje"), any());
    }

    // =====================================================
    //                       ACTIVAR
    // =====================================================

    @Test
    void activarProducto_exitoso_agregaMensajeYRedirige() {
        String vista = productoController.activarProducto(8, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(productoService).activar(8);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Producto activado correctamente");
    }

    @Test
    void activarProducto_conExcepcion_agregaErrorYRedirige() {
        doThrow(new RuntimeException("No se puede activar"))
                .when(productoService).activar(8);

        String vista = productoController.activarProducto(8, redirectAttrs);

        assertEquals(REDIRECT_GESTION, vista);
        verify(redirectAttrs).addFlashAttribute("error", "No se puede activar");
        verify(redirectAttrs, never()).addFlashAttribute(eq("mensaje"), any());
    }
}
