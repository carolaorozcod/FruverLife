package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Security.JwtService;
import com.FruverLifes.Service.GestorUsuarioService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock private GestorUsuarioService gestorUsuarioService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private LoginController controller;

    private MockHttpSession session;
    private MockHttpServletResponse response;
    private ConcurrentModel model;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        response = new MockHttpServletResponse();
        model = new ConcurrentModel();
    }

    private Persona persona(boolean habilitado, String hash, String... roles) {
        Persona p = new Persona();
        p.setId("u1");
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setUsuario("ana");
        acceso.setHabilitado(habilitado);
        acceso.setContrasenaHash(hash);
        acceso.setRoles(List.of(roles));
        p.setAccesoSistema(acceso);
        return p;
    }

    // ---------- Pantalla de login ----------
    @Test
    void mostrarLogin_devuelveLaVistaLogin() {
        assertEquals("login", controller.mostrarLogin());
    }

    // ---------- Login con error ----------
    @Test
    void login_usuarioNoExiste_vuelveAlLoginConError() {
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana")).thenReturn(Optional.empty());

        String vista = controller.login("ana", "1234", model, session, response);

        assertEquals("login", vista);
        assertEquals("Usuario o contraseña incorrectos", model.getAttribute("error"));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_contrasenaIncorrecta_noCreaSesionNiCookie() {
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana"))
                .thenReturn(Optional.of(persona(true, "hash", "GERENTE")));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        String vista = controller.login("ana", "mala", model, session, response);

        assertEquals("login", vista);
        assertEquals("Usuario o contraseña incorrectos", model.getAttribute("error"));
        assertNull(session.getAttribute("usuarioLogueado"));
        assertNull(response.getCookie("JWT"));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_usuarioDeshabilitado_noValidaLaContrasena() {
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana"))
                .thenReturn(Optional.of(persona(false, "hash", "GERENTE")));

        String vista = controller.login("ana", "1234", model, session, response);

        assertEquals("login", vista);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    // ---------- Login correcto ----------
    // Un mismo test que se repite con distintos datos (uno por rol)
    @ParameterizedTest
    @CsvSource({
            "GERENTE,redirect:/menu/gerente",
            "ADMINISTRADOR,redirect:/menu/administrador",
            "CAJERO,redirect:/menu/cajero"
    })
    void login_credencialesValidas_redirigeSegunElRol(String rol, String destinoEsperado) {
        Persona p = persona(true, "hash", rol);
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana")).thenReturn(Optional.of(p));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(jwtService.generarToken("ana", rol)).thenReturn("token-xyz");

        String vista = controller.login("ana", "1234", model, session, response);

        assertEquals(destinoEsperado, vista);
        assertSame(p, session.getAttribute("usuarioLogueado"));

        Cookie cookie = response.getCookie("JWT");
        assertNotNull(cookie);
        assertEquals("token-xyz", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());
    }

    @Test
    void login_sinRoles_cuentaComoCajero() {
        Persona p = persona(true, "hash");   // lista de roles vacía
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana")).thenReturn(Optional.of(p));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(jwtService.generarToken("ana", "CAJERO")).thenReturn("token");

        String vista = controller.login("ana", "1234", model, session, response);

        assertEquals("redirect:/menu/cajero", vista);
    }

    @Test
    void login_rolEnMinusculas_tambienSeReconoce() {
        Persona p = persona(true, "hash", "gerente");
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana")).thenReturn(Optional.of(p));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(jwtService.generarToken("ana", "gerente")).thenReturn("token");

        assertEquals("redirect:/menu/gerente", controller.login("ana", "1234", model, session, response));
    }

    // ---------- Logout e inicio ----------
    @Test
    void cerrarSesion_invalidaLaSesionYBorraLaCookie() {
        session.setAttribute("usuarioLogueado", new Persona());

        String vista = controller.cerrarSesion(session, response);

        assertEquals("redirect:/auth/login", vista);
        assertTrue(session.isInvalid());
        Cookie cookie = response.getCookie("JWT");
        assertNotNull(cookie);
        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    void inicio_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.inicio(session));
    }

    @Test
    void inicio_conSesion_muestraInicio() {
        session.setAttribute("usuarioLogueado", new Persona());

        assertEquals("inicio", controller.inicio(session));
    }
}
