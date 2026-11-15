package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.ProductoRespository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificarPrecioControllerTest {

    @Mock private ProductoRespository productoRepository;
    @InjectMocks private VerificarPrecioController controller;

    private MockHttpSession session;
    private RedirectAttributesModelMap redirectAttrs;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        redirectAttrs = new RedirectAttributesModelMap();
    }

    private void iniciarSesion() {
        session.setAttribute("usuarioLogueado", new Persona());
    }

    // ---------- abrirVista ----------
    @Test
    void abrirVista_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.abrirVista(session, new ConcurrentModel()));
    }

    @Test
    void abrirVista_conSesion_muestraLaPlantillaDelCajero() {
        iniciarSesion();
        ConcurrentModel model = new ConcurrentModel();

        String vista = controller.abrirVista(session, model);

        assertEquals("layout/PlantillaCajero", vista);
        assertEquals("VerificarPrecio", model.getAttribute("contenido"));
        assertNotNull(model.getAttribute("usuario"));
    }

    // ---------- buscarProducto ----------
    @Test
    void buscarProducto_sinSesion_redirigeAlLoginSinConsultar() {
        String vista = controller.buscarProducto("FR01", session, redirectAttrs);

        assertEquals("redirect:/auth/login", vista);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void buscarProducto_existe_loEnviaComoAtributoFlash() {
        iniciarSesion();
        Producto p = new Producto();
        when(productoRepository.findByCodigo("FR01")).thenReturn(p);

        String vista = controller.buscarProducto("  FR01  ", session, redirectAttrs);   // el código se recorta

        assertEquals("redirect:/menu/precio", vista);
        assertSame(p, redirectAttrs.getFlashAttributes().get("producto"));
        assertNull(redirectAttrs.getFlashAttributes().get("error"));
    }

    @Test
    void buscarProducto_noExiste_enviaMensajeDeError() {
        iniciarSesion();
        when(productoRepository.findByCodigo("ZZ")).thenReturn(null);

        String vista = controller.buscarProducto("ZZ", session, redirectAttrs);

        assertEquals("redirect:/menu/precio", vista);
        assertEquals("Producto no encontrado", redirectAttrs.getFlashAttributes().get("error"));
        assertNull(redirectAttrs.getFlashAttributes().get("producto"));
    }

    private Producto producto(String nombre, String codigo, String estado) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setCodigo(codigo);
        p.setEstado(estado);
        return p;
    }

    @Test
    void abrirVista_conSesion_ofreceSoloProductosActivosParaElBuscador() {
        iniciarSesion();
        Producto activo = producto("Manzana", "FR01", "ACTIVO");
        when(productoRepository.findAll()).thenReturn(List.of(activo, producto("Pera", "FR02", "INACTIVO")));
        ConcurrentModel model = new ConcurrentModel();

        controller.abrirVista(session, model);

        assertEquals(List.of(activo), model.getAttribute("productos"));
    }

    // ---------- productosActivos (lista JSON para el buscador por nombre) ----------
    @Test
    void productosActivos_sinSesion_devuelveListaVaciaSinConsultar() {
        assertTrue(controller.productosActivos(session).isEmpty());
        verifyNoInteractions(productoRepository);
    }

    @Test
    void productosActivos_conSesion_devuelveNombreYCodigoSoloDeLosActivos() {
        iniciarSesion();
        when(productoRepository.findAll()).thenReturn(List.of(
                producto("Manzana", "FR01", "ACTIVO"),
                producto("Pera", "FR02", "INACTIVO"),
                producto(null, null, "ACTIVO")));   // sin nombre ni código: salen como texto vacío

        List<Map<String, String>> resultado = controller.productosActivos(session);

        assertEquals(2, resultado.size());
        assertEquals(Map.of("nombre", "Manzana", "codigo", "FR01"), resultado.get(0));
        assertEquals(Map.of("nombre", "", "codigo", ""), resultado.get(1));
    }
}
