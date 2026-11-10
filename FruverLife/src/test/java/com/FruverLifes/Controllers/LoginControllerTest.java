package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Security.JwtService;
import com.FruverLifes.Service.GestorUsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private GestorUsuarioService gestorUsuarioService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Model model;

    @InjectMocks
    private LoginController loginController;

    // ---------- Helper ----------
    private Usuario crearUsuario(String nombre, String contrasena, String cargo, String estado) {
        Usuario user = mock(Usuario.class);
        when(user.getContrasena()).thenReturn(contrasena);
        when(user.getEstado()).thenReturn(estado);
        when(user.getUsuario()).thenReturn(nombre);
        when(user.getCargo()).thenReturn(cargo);
        return user;
    }

    // ---------- GET /auth/login ----------
    @Test
    void mostrarLogin_retornaVistaLogin() {
        assertEquals("login", loginController.mostrarLogin());
    }

    // ---------- POST /auth/login: casos exitosos ----------
    @Test
    void login_gerenteValido_redirigeAMenuGerenteYCreaCookie() {
        Usuario user = crearUsuario("ana", "1234", "Gerente", "ACTIVO");
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.of(user));
        when(jwtService.generarToken("ana", "Gerente")).thenReturn("token-abc");

        String vista = loginController.login("ana", "1234", model, session, response);

        assertEquals("redirect:/menu/gerente", vista);
        verify(session).setAttribute("usuarioLogueado", user);

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        Cookie cookie = captor.getValue();
        assertEquals("JWT", cookie.getName());
        assertEquals("token-abc", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());

        verify(model, never()).addAttribute(eq("error"), any());
    }

    @Test
    void login_administradorValido_redirigeAMenuAdministrador() {
        // También verifica que la comparación ignora mayúsculas/minúsculas
        Usuario user = crearUsuario("luis", "abcd", "ADMINISTRADOR", "ACTIVO");
        when(gestorUsuarioService.buscarPorUsuario("luis")).thenReturn(Optional.of(user));
        when(jwtService.generarToken("luis", "ADMINISTRADOR")).thenReturn("token-xyz");

        String vista = loginController.login("luis", "abcd", model, session, response);

        assertEquals("redirect:/menu/administrador", vista);
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    void login_cajeroValido_redirigeAMenuCajero() {
        Usuario user = crearUsuario("maria", "pass", "Cajero", "ACTIVO");
        when(gestorUsuarioService.buscarPorUsuario("maria")).thenReturn(Optional.of(user));
        when(jwtService.generarToken("maria", "Cajero")).thenReturn("token-123");

        String vista = loginController.login("maria", "pass", model, session, response);

        assertEquals("redirect:/menu/cajero", vista);
        verify(session).setAttribute("usuarioLogueado", user);
    }

    // ---------- POST /auth/login: casos de error ----------
    @Test
    void login_usuarioNoExiste_retornaLoginConError() {
        when(gestorUsuarioService.buscarPorUsuario("fantasma")).thenReturn(Optional.empty());

        String vista = loginController.login("fantasma", "1234", model, session, response);

        assertEquals("login", vista);
        verify(model).addAttribute("error", "Usuario o contraseña incorrectos");
        verify(jwtService, never()).generarToken(anyString(), anyString());
        verify(response, never()).addCookie(any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void login_contrasenaIncorrecta_retornaLoginConError() {
        Usuario user = mock(Usuario.class);
        when(user.getContrasena()).thenReturn("correcta");
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.of(user));

        String vista = loginController.login("ana", "incorrecta", model, session, response);

        assertEquals("login", vista);
        verify(model).addAttribute("error", "Usuario o contraseña incorrectos");
        verify(jwtService, never()).generarToken(anyString(), anyString());
        verify(response, never()).addCookie(any());
    }

    @Test
    void login_usuarioInactivo_retornaLoginConError() {
        Usuario user = mock(Usuario.class);
        when(user.getContrasena()).thenReturn("1234");
        when(user.getEstado()).thenReturn("INACTIVO");
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.of(user));

        String vista = loginController.login("ana", "1234", model, session, response);

        assertEquals("login", vista);
        verify(model).addAttribute("error", "Usuario o contraseña incorrectos");
        verify(jwtService, never()).generarToken(anyString(), anyString());
        verify(response, never()).addCookie(any());
    }

    // ---------- GET /auth/logout ----------
    @Test
    void cerrarSesion_invalidaSesionYEliminaCookie() {
        String vista = loginController.cerrarSesion(session, response);

        assertEquals("redirect:/auth/login", vista);
        verify(session).invalidate();

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        Cookie cookie = captor.getValue();
        assertEquals("JWT", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertEquals("/", cookie.getPath());
    }

    // ---------- GET /auth/inicio ----------
    @Test
    void inicio_sinSesion_redirigeALogin() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(null);

        assertEquals("redirect:/auth/login", loginController.inicio(session));
    }

    @Test
    void inicio_conSesion_retornaVistaInicio() {
        when(session.getAttribute("usuarioLogueado")).thenReturn(mock(Usuario.class));

        assertEquals("inicio", loginController.inicio(session));
    }
}
