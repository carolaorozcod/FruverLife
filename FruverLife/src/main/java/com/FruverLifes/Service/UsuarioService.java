package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService implements GestorUsuarioService {

    @Autowired
    private PersonasRepositories usuarioRepository;

    @Override
    public void registrarUsuarios(Persona usuario) {
        if (usuario.getAccesoSistema() != null && usuario.getAccesoSistema().getUsuario() != null) {
            usuario.getAccesoSistema().setUsuario(
                    usuario.getAccesoSistema().getUsuario().trim().toLowerCase());
        }
        usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Persona> buscarPorUsuario(String usuario) {
        if (usuario == null) return Optional.empty();

        String usuarioLimpio = usuario.trim().toLowerCase();
        return usuarioRepository.findByAccesoSistemaUsuario(usuarioLimpio);
    }

    @Override
    public Optional<Persona> buscarPorUsuarioActivo(String usuario) {
        if (usuario == null) return Optional.empty();

        String usuarioLimpio = usuario.trim().toLowerCase();
        return usuarioRepository
                .findByAccesoSistemaUsuarioAndAccesoSistemaEstado(usuarioLimpio, "ACTIVO");
    }

    @Override
    public Persona buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .filter(p -> p.getAccesoSistema() != null)
                .orElse(null);
    }

    @Override
    public void eliminarUsuario(String id) {
        cambiarEstado(id, "INACTIVO", false);
    }

    @Override
    public void activarUsuario(String id) {
        cambiarEstado(id, "ACTIVO", true);
    }

    private void cambiarEstado(String id, String estado, boolean habilitado) {
        Persona persona = buscarPorId(id);
        if (persona != null) {
            persona.getAccesoSistema().setEstado(estado);
            persona.getAccesoSistema().setHabilitado(habilitado);
            usuarioRepository.save(persona);
        }
    }

    @Override
    public List<Persona> listarTodosUsuarios() {
        return usuarioRepository.findByAccesoSistemaIsNotNull();
    }

    @Override
    public List<Persona> listarUsuarios() {
        return usuarioRepository.findByAccesoSistemaEstado("ACTIVO");
    }
}