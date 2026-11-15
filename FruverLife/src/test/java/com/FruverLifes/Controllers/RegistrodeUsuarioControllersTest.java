package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.GestorUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrodeUsuarioControllersTest {

    @Mock private GestorUsuarioService gestorUsuarioService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private RegistrodeUsuarioControllers controller;

    private RedirectAttributesModelMap redirectAttrs;

    @BeforeEach
    void setUp() {
        redirectAttrs = new RedirectAttributesModelMap();
    }

    // Arma el formulario tal como llega del navegador
    private Persona formulario(String id, String nombre, String identificacion,
                               String usuario, String contrasena, String... roles) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre(nombre);
        p.setIdentificacion(identificacion);
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setUsuario(usuario);
        acceso.setContrasenaHash(contrasena);   // aquí llega la contraseña en texto
        acceso.setRoles(new ArrayList<>(List.of(roles)));
        p.setAccesoSistema(acceso);
        return p;
    }

    // Persona ya guardada en la base
    private Persona personaGuardada(String id, String usuario) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre("Ana");
        p.setFunciones(new ArrayList<>(List.of("USUARIO")));
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setUsuario(usuario);
        acceso.setContrasenaHash("hashViejo");
        acceso.setEstado("ACTIVO");
        acceso.setHabilitado(true);
        p.setAccesoSistema(acceso);
        return p;
    }

    private Object flash(String clave) {
        return redirectAttrs.getFlashAttributes().get(clave);
    }

    private void assertErrorObligatorios(Persona form) {
        String vista = controller.guardarUsuario(form, redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Todos los campos son obligatorios", flash("error"));
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    // ---------- listar ----------
    @Test
    void listarUsuarios_redirigeAlMenu() {
        assertEquals("redirect:/menu/usuarios", controller.listarUsuarios());
    }

    // ---------- guardarUsuario: validaciones ----------
    @Test
    void guardarUsuario_sinAccesoAlSistema_muestraError() {
        assertErrorObligatorios(new Persona());
    }

    @Test
    void guardarUsuario_sinNombre_muestraError() {
        assertErrorObligatorios(formulario(null, " ", "123", "ana", "1234", "GERENTE"));
    }

    @Test
    void guardarUsuario_sinRoles_muestraError() {
        assertErrorObligatorios(formulario(null, "Ana", "123", "ana", "1234"));
    }

    @Test
    void guardarUsuario_nuevoSinContrasena_muestraError() {
        assertErrorObligatorios(formulario(null, "Ana", "123", "ana", "   ", "GERENTE"));
    }

    @Test
    void guardarUsuario_usuarioYaExisteEnOtraPersona_muestraError() {
        when(gestorUsuarioService.buscarPorUsuario("ana"))
                .thenReturn(Optional.of(personaGuardada("otro", "ana")));

        String vista = controller.guardarUsuario(
                formulario(null, "Ana", "123", "ana", "1234", "GERENTE"), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("El usuario ya existe", flash("error"));
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    // ---------- guardarUsuario: nuevo ----------
    @Test
    void guardarUsuario_nuevo_laCreaConDatosLimpiosYContrasenaCifrada() {
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hash1234");

        // llegan con espacios y mayúsculas: el controller los normaliza
        String vista = controller.guardarUsuario(
                formulario(null, "  Ana Pérez ", " 123 ", "  AnA ", " 1234 ", " GERENTE "), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Usuario registrado correctamente", flash("mensaje"));

        ArgumentCaptor<Persona> captor = ArgumentCaptor.forClass(Persona.class);
        verify(gestorUsuarioService).registrarUsuarios(captor.capture());
        Persona guardada = captor.getValue();

        assertEquals("Ana Pérez", guardada.getNombre());
        assertEquals("123", guardada.getIdentificacion());
        assertTrue(guardada.getFunciones().contains("USUARIO"));
        assertEquals("ana", guardada.getAccesoSistema().getUsuario());
        assertEquals("hash1234", guardada.getAccesoSistema().getContrasenaHash());
        assertEquals(List.of("GERENTE"), guardada.getAccesoSistema().getRoles());
        assertEquals("ACTIVO", guardada.getAccesoSistema().getEstado());
        assertTrue(guardada.getAccesoSistema().getHabilitado());
    }

    @Test
    void guardarUsuario_identificacionRepetida_muestraError() {
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hash");
        doThrow(new DuplicateKeyException("duplicado")).when(gestorUsuarioService).registrarUsuarios(any());

        String vista = controller.guardarUsuario(
                formulario(null, "Ana", "123", "ana", "1234", "GERENTE"), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Ya existe una persona con esa identificación", flash("error"));
        assertNull(flash("mensaje"));
    }

    // ---------- guardarUsuario: edición ----------
    @Test
    void guardarUsuario_edicionSinNuevaContrasena_conservaLaAnterior() {
        Persona existente = personaGuardada("p1", "ana");
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.of(existente));   // es la misma persona
        when(gestorUsuarioService.buscarPorId("p1")).thenReturn(existente);

        String vista = controller.guardarUsuario(
                formulario("p1", "Ana María", "123", "ana", "", "CAJERO"), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Usuario editado correctamente", flash("mensaje"));
        assertEquals("Ana María", existente.getNombre());
        assertEquals(List.of("CAJERO"), existente.getAccesoSistema().getRoles());
        assertEquals("hashViejo", existente.getAccesoSistema().getContrasenaHash());
        verifyNoInteractions(passwordEncoder);
        verify(gestorUsuarioService).registrarUsuarios(existente);
    }

    @Test
    void guardarUsuario_edicionConNuevaContrasena_laCifra() {
        Persona existente = personaGuardada("p1", "ana");
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.of(existente));
        when(gestorUsuarioService.buscarPorId("p1")).thenReturn(existente);
        when(passwordEncoder.encode("nueva")).thenReturn("hashNuevo");

        controller.guardarUsuario(formulario("p1", "Ana", "123", "ana", "nueva", "CAJERO"), redirectAttrs);

        assertEquals("hashNuevo", existente.getAccesoSistema().getContrasenaHash());
    }

    @Test
    void guardarUsuario_edicionDeUsuarioInexistente_muestraError() {
        when(gestorUsuarioService.buscarPorUsuario("ana")).thenReturn(Optional.empty());
        when(gestorUsuarioService.buscarPorId("p9")).thenReturn(null);

        String vista = controller.guardarUsuario(
                formulario("p9", "Ana", "123", "ana", "", "CAJERO"), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Usuario no encontrado", flash("error"));
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    // ---------- editar ----------
    @Test
    void editarUsuario_existe_cargaElFormulario() {
        ConcurrentModel model = new ConcurrentModel();
        Persona p = personaGuardada("p1", "ana");
        when(gestorUsuarioService.buscarPorId("p1")).thenReturn(p);
        when(gestorUsuarioService.listarTodosUsuarios()).thenReturn(List.of(p));

        String vista = controller.editarUsuario("p1", model, redirectAttrs);

        assertEquals("layout/PlantillaGerente", vista);
        assertSame(p, model.getAttribute("usuarioForm"));
        assertEquals(List.of(p), model.getAttribute("usuarios"));
        assertEquals("Registrousuario", model.getAttribute("contenido"));
    }

    @Test
    void editarUsuario_noExiste_redirigeConError() {
        when(gestorUsuarioService.buscarPorId("p9")).thenReturn(null);

        String vista = controller.editarUsuario("p9", new ConcurrentModel(), redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        assertEquals("Usuario no encontrado", flash("error"));
    }

    // ---------- eliminar / activar ----------
    @Test
    void eliminarUsuario_loInactivaYAvisa() {
        String vista = controller.eliminarUsuario("p1", redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(gestorUsuarioService).eliminarUsuario("p1");
        assertEquals("Usuario INACTIVO", flash("mensaje"));
    }

    @Test
    void activarUsuario_loActivaYAvisa() {
        String vista = controller.activarUsuario("p1", redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(gestorUsuarioService).activarUsuario("p1");
        assertEquals("Usuario ACTIVO", flash("mensaje"));
    }
}

