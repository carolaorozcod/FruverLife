package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrodeClientesControllerTest {

    @Mock private ClienteService clienteService;
    @InjectMocks private RegistrodeClientesController controller;

    private RedirectAttributesModelMap redirectAttrs;

    @BeforeEach
    void setUp() {
        redirectAttrs = new RedirectAttributesModelMap();
    }

    private Persona formulario(String id, String nombre, String identificacion, String correo) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre(nombre);
        p.setIdentificacion(identificacion);
        p.setCorreo(correo);
        p.setTelefono("300");
        return p;
    }

    private Object mensaje() {
        return redirectAttrs.getFlashAttributes().get("mensaje");
    }

    // ---------- guardarCliente: validaciones ----------
    @Test
    void guardarCliente_sinNombre_noGuarda() {
        String vista = controller.guardarCliente(formulario(null, "  ", "123", "a@b.com"), redirectAttrs);

        assertEquals("redirect:/menu/clientes", vista);
        assertEquals("Todos los campos obligatorios deben llenarse", mensaje());
        verifyNoInteractions(clienteService);
    }

    @Test
    void guardarCliente_sinIdentificacionOCorreo_noGuarda() {
        controller.guardarCliente(formulario(null, "Ana", null, "a@b.com"), redirectAttrs);
        assertEquals("Todos los campos obligatorios deben llenarse", mensaje());

        controller.guardarCliente(formulario(null, "Ana", "123", ""), redirectAttrs);
        verifyNoInteractions(clienteService);
    }

    // ---------- guardarCliente: nuevo ----------
    @Test
    void guardarCliente_nuevo_loRegistraConIdNulo() {
        Persona form = formulario("", "Ana", "123", "a@b.com");   // el formulario manda id vacío

        String vista = controller.guardarCliente(form, redirectAttrs);

        assertEquals("redirect:/menu/clientes", vista);
        assertNull(form.getId());
        verify(clienteService).guardar(form);
        assertEquals("Cliente registrado correctamente", mensaje());
    }

    @Test
    void guardarCliente_identificacionRepetida_muestraMensaje() {
        Persona form = formulario(null, "Ana", "123", "a@b.com");
        when(clienteService.guardar(form)).thenThrow(new DuplicateKeyException("duplicado"));

        String vista = controller.guardarCliente(form, redirectAttrs);

        assertEquals("redirect:/menu/clientes", vista);
        assertEquals("Ya existe una persona con esa identificación", mensaje());
    }

    // ---------- guardarCliente: edición ----------
    @Test
    void guardarCliente_edicion_actualizaLosCamposSinPerderLosDemasDatos() {
        Persona existente = formulario("c1", "Ana", "123", "viejo@b.com");
        existente.setPuntos(5000);   // este dato no debe perderse al editar
        when(clienteService.buscarPorId("c1")).thenReturn(existente);

        Persona form = formulario("c1", "Ana María", "456", "nuevo@b.com");
        form.setTelefono("311");

        String vista = controller.guardarCliente(form, redirectAttrs);

        assertEquals("redirect:/menu/clientes", vista);
        assertEquals("Ana María", existente.getNombre());
        assertEquals("456", existente.getIdentificacion());
        assertEquals("nuevo@b.com", existente.getCorreo());
        assertEquals("311", existente.getTelefono());
        assertEquals(5000, existente.getPuntos());
        verify(clienteService).guardar(existente);
        assertEquals("Cliente actualizado correctamente", mensaje());
    }

    @Test
    void guardarCliente_edicionDeClienteInexistente_muestraMensaje() {
        when(clienteService.buscarPorId("c9")).thenReturn(null);

        String vista = controller.guardarCliente(formulario("c9", "Ana", "123", "a@b.com"), redirectAttrs);

        assertEquals("redirect:/menu/clientes", vista);
        assertEquals("Cliente no encontrado", mensaje());
        verify(clienteService, never()).guardar(any());
    }

    // ---------- editarCliente ----------
    @Test
    void editarCliente_existe_cargaElFormulario() {
        ConcurrentModel model = new ConcurrentModel();
        Persona c = formulario("c1", "Ana", "123", "a@b.com");
        when(clienteService.buscarPorId("c1")).thenReturn(c);
        when(clienteService.listarClientes()).thenReturn(List.of(c));

        String vista = controller.editarCliente("c1", model);

        assertEquals("layout/PlantillaCajero", vista);
        assertSame(c, model.getAttribute("cliente"));
        assertEquals(List.of(c), model.getAttribute("clientes"));
        assertEquals("Cliente", model.getAttribute("contenido"));
    }

    @Test
    void editarCliente_noExiste_redirigeALaLista() {
        when(clienteService.buscarPorId("c9")).thenReturn(null);

        assertEquals("redirect:/menu/clientes", controller.editarCliente("c9", new ConcurrentModel()));
    }
}
