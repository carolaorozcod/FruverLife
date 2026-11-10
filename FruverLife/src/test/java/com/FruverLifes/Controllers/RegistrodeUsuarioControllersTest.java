package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Service.GestorUsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrodeUsuarioControllersTest {

    @Mock
    private GestorUsuarioService gestorUsuarioService;   // service falso

    @Mock
    private RedirectAttributes redirectAttrs;            // para los mensajes flash

    @Mock
    private Model model;

    @InjectMocks
    private RegistrodeUsuarioControllers controller;     // controller real con los mocks dentro

    // ---------- Método auxiliar para no repetir código ----------
    private Usuario crearUsuario(String usuario, String contrasena, String cargo) {
        Usuario u = new Usuario();
        u.setUsuario(usuario);
        u.setContrasena(contrasena);
        u.setCargo(cargo);
        return u;
    }

    // ---------- listarUsuarios ----------
    @Test
    void listarUsuarios_redirigeAlMenu() {
        String vista = controller.listarUsuarios();

        assertEquals("redirect:/menu/usuarios", vista);
    }

    // ---------- guardarUsuario ----------
    @Test
    void guardarUsuario_nuevoValido_registraYMuestraMensaje() {
        Usuario u = crearUsuario("ana", "1234", "Cajero");
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana"))
                .thenReturn(Optional.empty());           // no existe todavía

        String vista = controller.guardarUsuario(u, redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(gestorUsuarioService).registrarUsuarios(u);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Usuario registrado correctamente");
    }

    @Test
    void guardarUsuario_conEspacios_losQuitaAntesDeGuardar() {
        Usuario u = crearUsuario("  ana  ", "  1234  ", "  Cajero  ");
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana"))
                .thenReturn(Optional.empty());

        controller.guardarUsuario(u, redirectAttrs);

        // Capturamos lo que le llegó al service para revisar que ya venía "trimeado"
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(gestorUsuarioService).registrarUsuarios(captor.capture());

        assertEquals("ana", captor.getValue().getUsuario());
        assertEquals("1234", captor.getValue().getContrasena());
        assertEquals("Cajero", captor.getValue().getCargo());
    }

    @Test
    void guardarUsuario_conCampoVacio_muestraErrorYNoGuarda() {
        Usuario u = crearUsuario("ana", "   ", "Cajero");   // contraseña solo espacios

        String vista = controller.guardarUsuario(u, redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(redirectAttrs).addFlashAttribute("error", "Todos los campos son obligatorios");
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    @Test
    void guardarUsuario_conCampoNull_muestraErrorYNoGuarda() {
        Usuario u = crearUsuario(null, "1234", "Cajero");

        controller.guardarUsuario(u, redirectAttrs);

        verify(redirectAttrs).addFlashAttribute("error", "Todos los campos son obligatorios");
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    @Test
    void guardarUsuario_siYaExiste_muestraErrorYNoGuarda() {
        Usuario u = crearUsuario("ana", "1234", "Cajero");
        when(gestorUsuarioService.buscarPorUsuarioActivo("ana"))
                .thenReturn(Optional.of(new Usuario()));      // ya existe

        controller.guardarUsuario(u, redirectAttrs);

        verify(redirectAttrs).addFlashAttribute("error", "El usuario ya existe");
        verify(gestorUsuarioService, never()).registrarUsuarios(any());
    }

    @Test
    void guardarUsuario_edicion_guardaSinRevisarDuplicado() {
        Usuario u = crearUsuario("ana", "1234", "Cajero");
        u.setIdUsuario(5);        // tiene id => es edición (ajusta el tipo si tu id es Long)

        controller.guardarUsuario(u, redirectAttrs);

        verify(gestorUsuarioService, never()).buscarPorUsuarioActivo(anyString());
        verify(gestorUsuarioService).registrarUsuarios(u);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Usuario editado correctamente");
    }

    // ---------- editarUsuario ----------
    @Test
    void editarUsuario_cuandoExiste_cargaElFormulario() {
        Usuario u = crearUsuario("ana", "1234", "Cajero");
        when(gestorUsuarioService.buscarPorId(1)).thenReturn(u);
        when(gestorUsuarioService.listarUsuarios()).thenReturn(List.of(u));

        String vista = controller.editarUsuario(1, model, redirectAttrs);

        assertEquals("layout/PlantillaGerente", vista);
        verify(model).addAttribute("usuarioForm", u);
        verify(model).addAttribute("usuarios", List.of(u));
        verify(model).addAttribute("contenido", "Registrousuario");
    }

    @Test
    void editarUsuario_cuandoNoExiste_redirigeConError() {
        when(gestorUsuarioService.buscarPorId(99)).thenReturn(null);

        String vista = controller.editarUsuario(99, model, redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(redirectAttrs).addFlashAttribute("error", "Usuario no encontrado");
        verify(model, never()).addAttribute(anyString(), any());
    }

    // ---------- eliminarUsuario ----------
    @Test
    void eliminarUsuario_llamaAlServiceYMuestraMensaje() {
        String vista = controller.eliminarUsuario(3, redirectAttrs);

        assertEquals("redirect:/menu/usuarios", vista);
        verify(gestorUsuarioService).eliminarUsuario(3);
        verify(redirectAttrs).addFlashAttribute("mensaje", "Usuario INACTIVO");
    }
}