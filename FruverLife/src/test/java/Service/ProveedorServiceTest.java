package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock private PersonasRepositories repo;
    @InjectMocks private ProveedorService service;

    // Proveedor "nuevo": como llega del formulario (sin funciones, sin estado)
    private Persona proveedorNuevo(String nit) {
        Persona p = new Persona();
        p.setNombre("Frutas del Valle");
        Persona.DatosProveedor datos = new Persona.DatosProveedor();
        datos.setNit(nit);
        p.setDatosProveedor(datos);
        return p;
    }

    // Proveedor ya guardado (lista mutable, porque el service le quita funciones)
    private Persona proveedorGuardado(String id, String nit, String... funciones) {
        Persona p = new Persona();
        p.setId(id);
        p.setFunciones(new ArrayList<>(List.of(funciones)));
        Persona.DatosProveedor datos = new Persona.DatosProveedor();
        datos.setNit(nit);
        datos.setEstado("Activo");
        p.setDatosProveedor(datos);
        return p;
    }

    // ---------- guardar ----------
    @Test
    void guardar_sinDatosDeProveedor_lanzaError() {
        Persona p = new Persona();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(p));
        assertEquals("El NIT es obligatorio", ex.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void guardar_sinNit_lanzaError() {
        Persona p = proveedorNuevo(null);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(p));
        verifyNoInteractions(repo);
    }

    @Test
    void guardar_nitRepetido_lanzaError() {
        when(repo.existsByDatosProveedorNit("900123")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(proveedorNuevo("900123")));
        assertEquals("Ya existe un proveedor con el mismo nit : 900123", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void guardar_datosValidos_completaFuncionEstadoEIdentificacion() {
        Persona p = proveedorNuevo("900123");
        when(repo.existsByDatosProveedorNit("900123")).thenReturn(false);
        when(repo.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        Persona guardado = service.guardar(p);

        assertTrue(guardado.getFunciones().contains("PROVEEDOR"));
        assertEquals("Activo", guardado.getDatosProveedor().getEstado());
        assertEquals("900123", guardado.getIdentificacion());   // usa el NIT si no hay identificación
        verify(repo).save(p);
    }

    @Test
    void guardar_conIdentificacionPropia_laConserva() {
        Persona p = proveedorNuevo("900123");
        p.setIdentificacion("CC-55");
        when(repo.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        Persona guardado = service.guardar(p);

        assertEquals("CC-55", guardado.getIdentificacion());
    }

    // ---------- listar ----------
    @Test
    void listar_pideSoloLasPersonasConFuncionProveedor() {
        List<Persona> lista = List.of(proveedorGuardado("p1", "111", "PROVEEDOR"));
        when(repo.findByFuncionesContaining("PROVEEDOR")).thenReturn(lista);

        assertEquals(lista, service.listar());
    }

    // ---------- editar ----------
    @Test
    void editar_nitNuevoYaExiste_lanzaError() {
        when(repo.findById("p1")).thenReturn(Optional.of(proveedorGuardado("p1", "111", "PROVEEDOR")));
        when(repo.existsByDatosProveedorNit("222")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.editar("p1", proveedorNuevo("222")));
        assertEquals("Ya existe un proveedor con el mismo nit : 222", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void editar_datosValidos_actualizaLosCampos() {
        Persona existente = proveedorGuardado("p1", "111", "PROVEEDOR");
        Persona nuevos = proveedorNuevo("222");
        nuevos.setNombre("Nuevo Nombre");
        nuevos.setTelefono("300");
        nuevos.setCorreo("a@b.com");
        when(repo.findById("p1")).thenReturn(Optional.of(existente));
        when(repo.existsByDatosProveedorNit("222")).thenReturn(false);
        when(repo.save(existente)).thenReturn(existente);

        Persona resultado = service.editar("p1", nuevos);

        assertEquals("222", resultado.getDatosProveedor().getNit());
        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals("300", resultado.getTelefono());
        assertEquals("a@b.com", resultado.getCorreo());
    }

    @Test
    void editar_mismoNit_noConsultaDuplicados() {
        Persona existente = proveedorGuardado("p1", "111", "PROVEEDOR");
        when(repo.findById("p1")).thenReturn(Optional.of(existente));
        when(repo.save(existente)).thenReturn(existente);

        service.editar("p1", proveedorNuevo("111"));

        verify(repo, never()).existsByDatosProveedorNit(any());
    }

    @Test
    void editar_proveedorNoExiste_lanzaError() {
        when(repo.findById("x")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.editar("x", proveedorNuevo("222")));
        assertEquals("Proveedor no encontrado", ex.getMessage());
    }

    // ---------- activar / inactivar ----------
    @Test
    void inactivar_cambiaElEstadoAInactivo() {
        Persona p = proveedorGuardado("p1", "111", "PROVEEDOR");
        when(repo.findById("p1")).thenReturn(Optional.of(p));
        when(repo.save(p)).thenReturn(p);

        assertEquals("Inactivo", service.inactivar("p1").getDatosProveedor().getEstado());
    }

    @Test
    void activar_cambiaElEstadoAActivo() {
        Persona p = proveedorGuardado("p1", "111", "PROVEEDOR");
        p.getDatosProveedor().setEstado("Inactivo");
        when(repo.findById("p1")).thenReturn(Optional.of(p));
        when(repo.save(p)).thenReturn(p);

        assertEquals("Activo", service.activar("p1").getDatosProveedor().getEstado());
    }

    @Test
    void inactivar_personaQueNoEsProveedor_lanzaError() {
        when(repo.findById("p1")).thenReturn(Optional.of(proveedorGuardado("p1", "111", "CLIENTE")));

        assertThrows(RuntimeException.class, () -> service.inactivar("p1"));
        verify(repo, never()).save(any());
    }

    // ---------- eliminar ----------
    @Test
    void eliminar_siSoloEraProveedor_borraLaPersona() {
        Persona p = proveedorGuardado("p1", "111", "PROVEEDOR");
        when(repo.findById("p1")).thenReturn(Optional.of(p));

        service.eliminar("p1");

        verify(repo).deleteById("p1");
        verify(repo, never()).save(any());
    }

    @Test
    void eliminar_siTambienEsCliente_soloQuitaLaFuncion() {
        Persona p = proveedorGuardado("p1", "111", "PROVEEDOR", "CLIENTE");
        when(repo.findById("p1")).thenReturn(Optional.of(p));

        service.eliminar("p1");

        assertFalse(p.getFunciones().contains("PROVEEDOR"));
        assertNull(p.getDatosProveedor());
        verify(repo).save(p);
        verify(repo, never()).deleteById(any());
    }

    // ---------- existePorNit ----------
    @Test
    void existePorNit_delegaEnElRepositorio() {
        when(repo.existsByDatosProveedorNit("111")).thenReturn(true);

        assertTrue(service.existePorNit("111"));
    }
}

