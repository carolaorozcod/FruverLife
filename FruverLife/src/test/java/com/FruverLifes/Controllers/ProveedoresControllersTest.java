package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Proveedor;
import com.FruverLifes.Service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedoresControllersTest {

    @Mock
    private ProveedorService proveedorService;

    @InjectMocks
    private ProveedoresControllers proveedoresController;

    // =====================================================
    //                        LISTAR
    // =====================================================

    @Test
    void listar_retornaListaDelServicio() {
        List<Proveedor> proveedores = List.of(mock(Proveedor.class), mock(Proveedor.class));
        when(proveedorService.listar()).thenReturn(proveedores);

        List<Proveedor> resultado = proveedoresController.listar();

        assertEquals(proveedores, resultado);
        assertEquals(2, resultado.size());
        verify(proveedorService).listar();
    }

    // =====================================================
    //                        GUARDAR
    // =====================================================

    @Test
    void guardar_exitoso_retorna200ConMensaje() {
        Proveedor proveedor = mock(Proveedor.class);

        ResponseEntity<String> respuesta = proveedoresController.guardar(proveedor);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Proveedor registrado correctamente", respuesta.getBody());
        verify(proveedorService).guardar(proveedor);
    }

    @Test
    void guardar_conIllegalArgument_retorna409ConMensajeDeError() {
        Proveedor proveedor = mock(Proveedor.class);
        doThrow(new IllegalArgumentException("El proveedor ya existe"))
                .when(proveedorService).guardar(proveedor);

        ResponseEntity<String> respuesta = proveedoresController.guardar(proveedor);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("El proveedor ya existe", respuesta.getBody());
    }

    // =====================================================
    //                        EDITAR
    // =====================================================

    @Test
    void editar_llamaAlServicioYRetornaMensaje() {
        Proveedor proveedor = mock(Proveedor.class);

        String respuesta = proveedoresController.editar(4, proveedor);

        assertEquals("Proveedor actualizado correctamente", respuesta);
        verify(proveedorService).editar(4, proveedor);
    }

    @Test
    void editar_conExcepcion_laPropagaPorqueElControladorNoLaCaptura() {
        Proveedor proveedor = mock(Proveedor.class);
        doThrow(new RuntimeException("Proveedor no encontrado"))
                .when(proveedorService).editar(99, proveedor);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> proveedoresController.editar(99, proveedor));

        assertEquals("Proveedor no encontrado", ex.getMessage());
    }

    // =====================================================
    //                       ELIMINAR
    // =====================================================

    @Test
    void eliminar_exitoso_retorna200ConMensaje() {
        ResponseEntity<String> respuesta = proveedoresController.eliminar(1);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Proveedor eliminado correctamente", respuesta.getBody());
        verify(proveedorService).eliminar(1);
    }

    @Test
    void eliminar_conProductosAsociados_retorna409() {
        doThrow(new IllegalStateException("Tiene productos asociados"))
                .when(proveedorService).eliminar(1);

        ResponseEntity<String> respuesta = proveedoresController.eliminar(1);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Tiene productos asociados", respuesta.getBody());
    }

    @Test
    void eliminar_proveedorNoExiste_retorna404() {
        doThrow(new RuntimeException("Proveedor no encontrado"))
                .when(proveedorService).eliminar(99);

        ResponseEntity<String> respuesta = proveedoresController.eliminar(99);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Proveedor no encontrado", respuesta.getBody());
    }

    // =====================================================
    //                       INACTIVAR
    // =====================================================

    @Test
    void inactivar_exitoso_retorna200ConMensaje() {
        ResponseEntity<String> respuesta = proveedoresController.inactivar(2);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Proveedor inactivado correctamente", respuesta.getBody());
        verify(proveedorService).inactivar(2);
    }

    @Test
    void inactivar_proveedorNoExiste_retorna404() {
        doThrow(new RuntimeException("Proveedor no encontrado"))
                .when(proveedorService).inactivar(99);

        ResponseEntity<String> respuesta = proveedoresController.inactivar(99);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Proveedor no encontrado", respuesta.getBody());
    }

    // =====================================================
    //                        ACTIVAR
    // =====================================================

    @Test
    void activar_exitoso_retorna200ConMensaje() {
        ResponseEntity<String> respuesta = proveedoresController.activar(2);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Proveedor activado correctamente", respuesta.getBody());
        verify(proveedorService).activar(2);
    }

    @Test
    void activar_proveedorNoExiste_retorna404() {
        doThrow(new RuntimeException("Proveedor no encontrado"))
                .when(proveedorService).activar(99);

        ResponseEntity<String> respuesta = proveedoresController.activar(99);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Proveedor no encontrado", respuesta.getBody());
    }
}