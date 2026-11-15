package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioControllerTest {

    @Mock private InventarioService inventarioService;
    @InjectMocks private InventarioController controller;

    // Objetos reales y livianos de Spring: así no hay que "mockear" la sesión ni los mensajes flash
    private MockHttpSession session;
    private RedirectAttributesModelMap redirectAttrs;
    private Compra compra;
    private Persona usuario;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        redirectAttrs = new RedirectAttributesModelMap();
        compra = new Compra();
        usuario = new Persona();
        usuario.setId("u1");
    }

    @Test
    void ingresarMercancia_sinUsuarioEnSesion_redirigeAlLogin() {
        String vista = controller.ingresarMercancia(compra, session, redirectAttrs);

        assertEquals("redirect:/auth/login", vista);
        verifyNoInteractions(inventarioService);
        assertTrue(redirectAttrs.getFlashAttributes().isEmpty());
    }

    @Test
    void ingresarMercancia_conUsuario_registraLaCompraConSuId() {
        session.setAttribute("usuarioLogueado", usuario);

        String vista = controller.ingresarMercancia(compra, session, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        verify(inventarioService).registrarCompra(compra, "u1");
        assertEquals("Inventario actualizado con éxito.", redirectAttrs.getFlashAttributes().get("mensaje"));
    }

    @Test
    void ingresarMercancia_siElServiceFalla_muestraElMensajeDeError() {
        session.setAttribute("usuarioLogueado", usuario);
        when(inventarioService.registrarCompra(compra, "u1"))
                .thenThrow(new RuntimeException("Proveedor no encontrado"));

        String vista = controller.ingresarMercancia(compra, session, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        assertEquals("Proveedor no encontrado", redirectAttrs.getFlashAttributes().get("error"));
        assertFalse(redirectAttrs.getFlashAttributes().containsKey("mensaje"));
    }
}
