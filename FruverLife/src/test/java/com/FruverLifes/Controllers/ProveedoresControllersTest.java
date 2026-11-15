package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedoresControllersTest {

    @Mock private ProveedorService proveedorService;
    @InjectMocks private ProveedoresControllers controller;

    private final Persona proveedor = new Persona();

    // Este controller devuelve ResponseEntity: revisamos código HTTP y cuerpo
    private void assertRespuesta(int codigo, String cuerpo, ResponseEntity<String> respuesta) {
        assertEquals(codigo, respuesta.getStatusCode().value());
        assertEquals(cuerpo, respuesta.getBody());
    }

    // ---------- listar ----------
    @Test
    void listar_devuelveLosProveedores() {
        when(proveedorService.listar()).thenReturn(List.of(proveedor));

        assertEquals(List.of(proveedor), controller.listar());
    }

    // ---------- guardar ----------
    @Test
    void guardar_ok_devuelve200() {
        assertRespuesta(200, "Proveedor registrado correctamente", controller.guardar(proveedor));
        verify(proveedorService).guardar(proveedor);
    }

    @Test
    void guardar_nitRepetido_devuelve409() {
        when(proveedorService.guardar(proveedor))
                .thenThrow(new IllegalArgumentException("Ya existe un proveedor con el mismo nit : 900"));

        assertRespuesta(409, "Ya existe un proveedor con el mismo nit : 900", controller.guardar(proveedor));
    }

    // ---------- editar ----------
    @Test
    void editar_ok_devuelve200() {
        assertRespuesta(200, "Proveedor actualizado correctamente", controller.editar("p1", proveedor));
        verify(proveedorService).editar("p1", proveedor);
    }

    @Test
    void editar_nitRepetido_devuelve409() {
        when(proveedorService.editar("p1", proveedor)).thenThrow(new IllegalArgumentException("NIT repetido"));

        assertRespuesta(409, "NIT repetido", controller.editar("p1", proveedor));
    }

    @Test
    void editar_proveedorNoExiste_devuelve404() {
        when(proveedorService.editar("p1", proveedor)).thenThrow(new RuntimeException("Proveedor no encontrado"));

        assertRespuesta(404, "Proveedor no encontrado", controller.editar("p1", proveedor));
    }

    // ---------- eliminar ----------
    @Test
    void eliminar_ok_devuelve200() {
        assertRespuesta(200, "Proveedor eliminado correctamente", controller.eliminar("p1"));
        verify(proveedorService).eliminar("p1");
    }

    @Test
    void eliminar_conProductosAsociados_devuelve409() {
        doThrow(new IllegalStateException("Tiene productos asociados")).when(proveedorService).eliminar("p1");

        assertRespuesta(409, "Tiene productos asociados", controller.eliminar("p1"));
    }

    @Test
    void eliminar_proveedorNoExiste_devuelve404() {
        doThrow(new RuntimeException("Proveedor no encontrado")).when(proveedorService).eliminar("p1");

        assertRespuesta(404, "Proveedor no encontrado", controller.eliminar("p1"));
    }

    // ---------- inactivar / activar ----------
    @Test
    void inactivar_ok_devuelve200() {
        assertRespuesta(200, "Proveedor inactivado correctamente", controller.inactivar("p1"));
        verify(proveedorService).inactivar("p1");
    }

    @Test
    void inactivar_proveedorNoExiste_devuelve404() {
        when(proveedorService.inactivar("p1")).thenThrow(new RuntimeException("Proveedor no encontrado"));

        assertRespuesta(404, "Proveedor no encontrado", controller.inactivar("p1"));
    }

    @Test
    void activar_ok_devuelve200() {
        assertRespuesta(200, "Proveedor activado correctamente", controller.activar("p1"));
        verify(proveedorService).activar("p1");
    }

    @Test
    void activar_proveedorNoExiste_devuelve404() {
        when(proveedorService.activar("p1")).thenThrow(new RuntimeException("Proveedor no encontrado"));

        assertRespuesta(404, "Proveedor no encontrado", controller.activar("p1"));
    }
}
