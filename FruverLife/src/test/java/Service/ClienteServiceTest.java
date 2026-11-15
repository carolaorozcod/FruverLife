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
class ClienteServiceTest {

    @Mock private PersonasRepositories personasRepository;
    @InjectMocks private ClienteService service;

    private Persona persona(String id, String... funciones) {
        Persona p = new Persona();
        p.setId(id);
        p.setFunciones(new ArrayList<>(List.of(funciones)));
        return p;
    }

    @Test
    void listarClientes_pideLasPersonasConFuncionCliente() {
        List<Persona> lista = List.of(persona("c1", "CLIENTE"));
        when(personasRepository.findByFuncionesContaining("CLIENTE")).thenReturn(lista);

        assertEquals(lista, service.listarClientes());
    }

    @Test
    void buscarPorId_existe_devuelveElCliente() {
        Persona c = persona("c1", "CLIENTE");
        when(personasRepository.findByIdAndFuncionesContaining("c1", "CLIENTE"))
                .thenReturn(Optional.of(c));

        assertSame(c, service.buscarPorId("c1"));
    }

    @Test
    void buscarPorId_noExiste_devuelveNull() {
        when(personasRepository.findByIdAndFuncionesContaining("x", "CLIENTE"))
                .thenReturn(Optional.empty());

        assertNull(service.buscarPorId("x"));
    }

    @Test
    void buscarPorIdentificacion_siEsCliente_loDevuelve() {
        Persona c = persona("c1", "CLIENTE");
        when(personasRepository.findByIdentificacion("123")).thenReturn(Optional.of(c));

        assertSame(c, service.buscarPorIdentificacion("123"));
    }

    @Test
    void buscarPorIdentificacion_siNoEsCliente_devuelveNull() {
        Persona proveedor = persona("p1", "PROVEEDOR");
        when(personasRepository.findByIdentificacion("123")).thenReturn(Optional.of(proveedor));

        assertNull(service.buscarPorIdentificacion("123"));
    }

    @Test
    void buscarPorIdentificacion_conFuncionesNulas_devuelveNull() {
        Persona sinFunciones = new Persona();
        when(personasRepository.findByIdentificacion("123")).thenReturn(Optional.of(sinFunciones));

        assertNull(service.buscarPorIdentificacion("123"));
    }

    @Test
    void guardar_sinFunciones_creaLaListaYAgregaCliente() {
        Persona p = new Persona();
        when(personasRepository.save(p)).thenReturn(p);

        Persona guardado = service.guardar(p);

        assertEquals(List.of("CLIENTE"), guardado.getFunciones());
    }

    @Test
    void guardar_siYaTieneLaFuncion_noLaDuplica() {
        Persona p = persona("c1", "CLIENTE");
        when(personasRepository.save(p)).thenReturn(p);

        service.guardar(p);

        assertEquals(1, p.getFunciones().size());
    }

    @Test
    void guardar_conOtraFuncion_agregaClienteSinPerderLaOtra() {
        Persona p = persona("p1", "PROVEEDOR");
        when(personasRepository.save(p)).thenReturn(p);

        service.guardar(p);

        assertTrue(p.getFunciones().containsAll(List.of("PROVEEDOR", "CLIENTE")));
    }

    @Test
    void eliminar_siNoExiste_noHaceNada() {
        when(personasRepository.findById("x")).thenReturn(Optional.empty());

        service.eliminar("x");

        verify(personasRepository, never()).deleteById(any());
        verify(personasRepository, never()).save(any());
    }

    @Test
    void eliminar_siSoloEraCliente_borraLaPersona() {
        when(personasRepository.findById("c1")).thenReturn(Optional.of(persona("c1", "CLIENTE")));

        service.eliminar("c1");

        verify(personasRepository).deleteById("c1");
        verify(personasRepository, never()).save(any());
    }

    @Test
    void eliminar_siTieneOtrasFunciones_soloQuitaCliente() {
        Persona p = persona("c1", "CLIENTE", "PROVEEDOR");
        when(personasRepository.findById("c1")).thenReturn(Optional.of(p));

        service.eliminar("c1");

        assertEquals(List.of("PROVEEDOR"), p.getFunciones());
        verify(personasRepository).save(p);
        verify(personasRepository, never()).deleteById(any());
    }
}
