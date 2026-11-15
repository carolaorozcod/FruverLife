package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private PersonasRepositories usuarioRepository;
    @InjectMocks private UsuarioService service;

    private Persona personaConAcceso(String id, String usuario, String estado) {
        Persona p = new Persona();
        p.setId(id);
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setUsuario(usuario);
        acceso.setEstado(estado);
        acceso.setHabilitado("ACTIVO".equals(estado));
        p.setAccesoSistema(acceso);
        return p;
    }

    // ---------- registrarUsuarios ----------
    @Test
    void registrarUsuarios_normalizaElNombreDeUsuarioYGuarda() {
        Persona p = personaConAcceso(null, "  AnA  ", "ACTIVO");

        service.registrarUsuarios(p);

        assertEquals("ana", p.getAccesoSistema().getUsuario());
        verify(usuarioRepository).save(p);
    }

    @Test
    void registrarUsuarios_sinAcceso_guardaSinFallar() {
        Persona p = new Persona();

        service.registrarUsuarios(p);

        verify(usuarioRepository).save(p);
    }

    // ---------- buscarPorUsuario ----------
    @Test
    void buscarPorUsuario_nulo_devuelveVacioSinConsultar() {
        assertTrue(service.buscarPorUsuario(null).isEmpty());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void buscarPorUsuario_limpiaElTextoAntesDeConsultar() {
        Persona p = personaConAcceso("u1", "ana", "ACTIVO");
        when(usuarioRepository.findByAccesoSistemaUsuario("ana")).thenReturn(Optional.of(p));

        Optional<Persona> resultado = service.buscarPorUsuario("  ANA ");

        assertTrue(resultado.isPresent());
        assertSame(p, resultado.get());
    }

    @Test
    void buscarPorUsuarioActivo_nulo_devuelveVacio() {
        assertTrue(service.buscarPorUsuarioActivo(null).isEmpty());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void buscarPorUsuarioActivo_consultaConEstadoActivo() {
        Persona p = personaConAcceso("u1", "ana", "ACTIVO");
        when(usuarioRepository.findByAccesoSistemaUsuarioAndAccesoSistemaEstado("ana", "ACTIVO"))
                .thenReturn(Optional.of(p));

        assertSame(p, service.buscarPorUsuarioActivo(" Ana ").orElseThrow());
    }

    // ---------- buscarPorId ----------
    @Test
    void buscarPorId_conAcceso_devuelveLaPersona() {
        Persona p = personaConAcceso("u1", "ana", "ACTIVO");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(p));

        assertSame(p, service.buscarPorId("u1"));
    }

    @Test
    void buscarPorId_personaSinAccesoAlSistema_devuelveNull() {
        Persona soloCliente = new Persona();
        soloCliente.setId("c1");
        when(usuarioRepository.findById("c1")).thenReturn(Optional.of(soloCliente));

        assertNull(service.buscarPorId("c1"));
    }

    @Test
    void buscarPorId_noExiste_devuelveNull() {
        when(usuarioRepository.findById("x")).thenReturn(Optional.empty());

        assertNull(service.buscarPorId("x"));
    }

    // ---------- eliminar / activar ----------
    @Test
    void eliminarUsuario_loDejaInactivoYDeshabilitado() {
        Persona p = personaConAcceso("u1", "ana", "ACTIVO");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(p));

        service.eliminarUsuario("u1");

        assertEquals("INACTIVO", p.getAccesoSistema().getEstado());
        assertFalse(p.getAccesoSistema().getHabilitado());
        verify(usuarioRepository).save(p);
    }

    @Test
    void activarUsuario_loDejaActivoYHabilitado() {
        Persona p = personaConAcceso("u1", "ana", "INACTIVO");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(p));

        service.activarUsuario("u1");

        assertEquals("ACTIVO", p.getAccesoSistema().getEstado());
        assertTrue(p.getAccesoSistema().getHabilitado());
        verify(usuarioRepository).save(p);
    }

    @Test
    void eliminarUsuario_siNoExiste_noGuardaNada() {
        when(usuarioRepository.findById("x")).thenReturn(Optional.empty());

        service.eliminarUsuario("x");

        verify(usuarioRepository, never()).save(any());
    }

    // ---------- listados ----------
    @Test
    void listarTodosUsuarios_devuelveLosQueTienenAcceso() {
        List<Persona> lista = List.of(personaConAcceso("u1", "ana", "ACTIVO"));
        when(usuarioRepository.findByAccesoSistemaIsNotNull()).thenReturn(lista);

        assertEquals(lista, service.listarTodosUsuarios());
    }

    @Test
    void listarUsuarios_devuelveSoloLosActivos() {
        List<Persona> lista = List.of(personaConAcceso("u1", "ana", "ACTIVO"));
        when(usuarioRepository.findByAccesoSistemaEstado("ACTIVO")).thenReturn(lista);

        assertEquals(lista, service.listarUsuarios());
    }
}

