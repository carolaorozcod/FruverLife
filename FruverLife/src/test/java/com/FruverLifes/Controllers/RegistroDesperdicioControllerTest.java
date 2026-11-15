package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Service.GestorUsuarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.RegistroDesperdicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroDesperdicioControllerTest {

    @Mock private Productoservice productoService;
    @Mock private RegistroDesperdicioService registroService;
    @Mock private GestorUsuarioService gestorUsuarioService;
    @InjectMocks private RegistroDesperdicioController controller;

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

    // ---------- abrirVista ----------
    @Test
    void abrirVista_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.abrirVista(session, model));
        verifyNoInteractions(registroService);
    }

    @Test
    void abrirVista_conSesion_cargaLosDatosDeLaPantalla() {
        iniciarSesion();
        RegistroDesperdicio r = new RegistroDesperdicio();
        r.setUsuarioId("u1");
        when(registroService.listar()).thenReturn(List.of(r));
        when(registroService.total()).thenReturn(5000.0);
        when(gestorUsuarioService.buscarPorId("u1")).thenReturn(usuario);

        String vista = controller.abrirVista(session, model);

        assertEquals("layout/plantillaAdministrador", vista);
        assertEquals("Registrodesperdicios", model.getAttribute("contenido"));
        assertEquals(5000.0, model.getAttribute("totalGeneral"));
        assertEquals(List.of(r), model.getAttribute("registros"));
        Map<?, ?> responsables = (Map<?, ?>) model.getAttribute("responsables");
        assertEquals("Ana", responsables.get("u1"));
    }

    @Test
    void abrirVista_responsableNoEncontrado_muestraDesconocido() {
        iniciarSesion();
        RegistroDesperdicio r = new RegistroDesperdicio();
        r.setUsuarioId("u9");
        when(registroService.listar()).thenReturn(List.of(r));
        when(gestorUsuarioService.buscarPorId("u9")).thenReturn(null);

        controller.abrirVista(session, model);

        Map<?, ?> responsables = (Map<?, ?>) model.getAttribute("responsables");
        assertEquals("Desconocido", responsables.get("u9"));
    }

    // ---------- buscarProducto ----------
    @Test
    void buscarProducto_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.buscarProducto("FR01", session, model));
        verifyNoInteractions(productoService);
    }

    @Test
    void buscarProducto_existe_muestraMensajeYProducto() {
        iniciarSesion();
        Producto p = new Producto();
        when(productoService.buscarPorcodigo("FR01")).thenReturn(p);

        String vista = controller.buscarProducto("  FR01 ", session, model);

        assertEquals("layout/plantillaAdministrador", vista);
        assertEquals("Producto encontrado correctamente", model.getAttribute("mensaje"));
        assertSame(p, model.getAttribute("producto"));
    }

    @Test
    void buscarProducto_noExiste_muestraError() {
        iniciarSesion();
        when(productoService.buscarPorcodigo("ZZ")).thenThrow(new RuntimeException("no existe"));

        controller.buscarProducto("ZZ", session, model);

        assertEquals("Producto no encontrado", model.getAttribute("error"));
        assertNull(model.getAttribute("producto"));
    }

    // ---------- guardarRegistro ----------
    @Test
    void guardarRegistro_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.guardarRegistro("FR01", 3.0, "Vencido", session, model));
        verifyNoInteractions(registroService);
    }

    @Test
    void guardarRegistro_ok_muestraMensajeDeExito() {
        iniciarSesion();

        String vista = controller.guardarRegistro("FR01", 3.0, "Vencido", session, model);

        assertEquals("layout/plantillaAdministrador", vista);
        verify(registroService).guardar("FR01", 3.0, "Vencido", usuario);
        assertEquals("¡Registro guardado con éxito!", model.getAttribute("mensaje"));
        assertNull(model.getAttribute("error"));
    }

    @Test
    void guardarRegistro_siElServiceFalla_muestraElError() {
        iniciarSesion();
        when(registroService.guardar("FR01", 30.0, "Vencido", usuario))
                .thenThrow(new RuntimeException("No hay suficiente stock. Disponible: 2.0"));

        controller.guardarRegistro("FR01", 30.0, "Vencido", session, model);

        assertEquals("No hay suficiente stock. Disponible: 2.0", model.getAttribute("error"));
        assertNull(model.getAttribute("mensaje"));
    }

    @Test
    void abrirVista_conSesion_ofreceSoloLosProductosActivos() {
        iniciarSesion();
        Producto activo = new Producto();
        activo.setEstado("ACTIVO");
        Producto inactivo = new Producto();
        inactivo.setEstado("INACTIVO");
        when(productoService.listarProductos()).thenReturn(List.of(activo, inactivo));

        controller.abrirVista(session, model);

        assertEquals(List.of(activo), model.getAttribute("productos"));
    }
}
