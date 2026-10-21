package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Clientes;
import com.FruverLifes.Service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrodeclienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private RegistrodeClientesController  clienteController;

    @Test
    void testGuardarClienteConCamposVacios() {
        Clientes cliente = new Clientes();
        cliente.setNombre("");
        cliente.setNumeroCe("");
        cliente.setCorreo("");

        String vista = clienteController.guardarCliente(cliente, redirectAttributes);

        assertEquals("redirect:/menu/clientes", vista);
        verify(clienteService, never()).guardar(any());
    }

    @Test
    void testGuardarClienteExitoso() {
        Clientes cliente = new Clientes();
        cliente.setNombre("Carlos Pérez");
        cliente.setNumeroCe("12345678");
        cliente.setCorreo("carlos@test.com");

        String vista = clienteController.guardarCliente(cliente, redirectAttributes);

        assertEquals("redirect:/menu/clientes", vista);
        verify(clienteService, times(1)).guardar(cliente);
    }
}