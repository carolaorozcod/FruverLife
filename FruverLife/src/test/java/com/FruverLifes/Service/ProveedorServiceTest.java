package com.FruverLifes.Service;

import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Repositories.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    @Test
    void testGuardarProveedorNuevoExitoso() {

        Proveedor proveedor = new Proveedor();
        proveedor.setNit("900123456");
        proveedor.setNombre("Frutas del Campo S.A.S.");

        when(proveedorRepository.existsByNit("900123456")).thenReturn(false);
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);


        Proveedor resultado = proveedorService.guardar(proveedor);

        assertNotNull(resultado);
        assertEquals("900123456", resultado.getNit());
        verify(proveedorRepository, times(1)).save(proveedor);
    }

    @Test
    void testGuardarProveedorNitDuplicadoLanzaExcepcion() {

        Proveedor proveedor = new Proveedor();
        proveedor.setNit("900123456");

        when(proveedorRepository.existsByNit("900123456")).thenReturn(true);


        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            proveedorService.guardar(proveedor);
        });

        assertTrue(exception.getMessage().contains("Ya existe un proveedor con el mismo nit"));
        verify(proveedorRepository, never()).save(any());
    }
}