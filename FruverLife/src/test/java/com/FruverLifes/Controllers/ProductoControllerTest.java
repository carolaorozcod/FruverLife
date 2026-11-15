package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Producto;
import com.FruverLifes.Service.Productoservice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock private Productoservice productoService;
    @InjectMocks private ProductoController controller;

    private RedirectAttributesModelMap redirectAttrs;
    private Producto producto;

    @BeforeEach
    void setUp() {
        redirectAttrs = new RedirectAttributesModelMap();
        producto = new Producto();
        producto.setId("p1");
    }

    private Object flash(String clave) {
        return redirectAttrs.getFlashAttributes().get(clave);
    }

    // ---------- guardar ----------
    @Test
    void guardarProducto_ok_muestraMensaje() {
        String vista = controller.guardarProducto(producto, redirectAttrs);

        assertEquals("redirect:/menu/gestion", vista);
        verify(productoService).guardarProducto(producto);
        assertEquals("Producto guardado exitosamente", flash("mensaje"));
    }

    @Test
    void guardarProducto_siElServiceFalla_muestraElError() {
        when(productoService.guardarProducto(producto))
                .thenThrow(new IllegalArgumentException("Ya existe un producto registrado con el código FR01"));

        String vista = controller.guardarProducto(producto, redirectAttrs);

        assertEquals("redirect:/menu/gestion", vista);
        assertEquals("Ya existe un producto registrado con el código FR01", flash("error"));
        assertNull(flash("mensaje"));
    }

    // ---------- editar ----------
    @Test
    void editarProducto_cargaElFormularioDeGestion() {
        ConcurrentModel model = new ConcurrentModel();
        when(productoService.buscarPorId("p1")).thenReturn(producto);
        when(productoService.listarProductos()).thenReturn(List.of(producto));

        String vista = controller.editarProducto("p1", model);

        assertEquals("layout/plantillaAdministrador", vista);
        assertSame(producto, model.getAttribute("producto"));
        assertEquals(List.of(producto), model.getAttribute("productos"));
        assertEquals("GestionStock", model.getAttribute("contenido"));
    }

    @Test
    void actualizarProducto_ok_muestraMensaje() {
        String vista = controller.actualizarProducto("p1", producto, redirectAttrs);

        assertEquals("redirect:/menu/gestion", vista);
        verify(productoService).editarProducto("p1", producto);
        assertEquals("Producto actualizado exitosamente", flash("mensaje"));
    }

    @Test
    void actualizarProducto_siElServiceFalla_muestraElError() {
        when(productoService.editarProducto("p1", producto))
                .thenThrow(new RuntimeException("Producto no encontrado"));

        controller.actualizarProducto("p1", producto, redirectAttrs);

        assertEquals("Producto no encontrado", flash("error"));
    }

    // ---------- inactivar / activar ----------
    @Test
    void inactivarProducto_ok_muestraMensaje() {
        String vista = controller.inactivarProducto("p1", redirectAttrs);

        assertEquals("redirect:/menu/gestion", vista);
        verify(productoService).inactivar("p1");
        assertEquals("Producto inactivado correctamente", flash("mensaje"));
    }

    @Test
    void inactivarProducto_siFalla_muestraElError() {
        when(productoService.inactivar("p1")).thenThrow(new RuntimeException("Producto no encontrado"));

        controller.inactivarProducto("p1", redirectAttrs);

        assertEquals("Producto no encontrado", flash("error"));
    }

    @Test
    void activarProducto_ok_muestraMensaje() {
        String vista = controller.activarProducto("p1", redirectAttrs);

        assertEquals("redirect:/menu/gestion", vista);
        verify(productoService).activar("p1");
        assertEquals("Producto activado correctamente", flash("mensaje"));
    }

    @Test
    void activarProducto_siFalla_muestraElError() {
        when(productoService.activar("p1")).thenThrow(new RuntimeException("Producto no encontrado"));

        controller.activarProducto("p1", redirectAttrs);

        assertEquals("Producto no encontrado", flash("error"));
    }

    // ---------- informe ----------
    @Test
    void generarInforme_cargaAgotadosYRecientes() {
        ConcurrentModel model = new ConcurrentModel();
        List<Producto> agotados = List.of(producto);
        when(productoService.listarProductosAgotandose()).thenReturn(agotados);
        when(productoService.listarIngresosRecientes()).thenReturn(List.of());

        String vista = controller.generarInforme(model);

        assertEquals("/menu/informeMer", vista);
        assertEquals(agotados, model.getAttribute("productosAgotados"));
        assertEquals(List.of(), model.getAttribute("productosRecientes"));
    }
}
