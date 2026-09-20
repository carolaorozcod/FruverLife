package com.FruverLifes.Service;

import com.FruverLifes.Model.Usuario;
import com.FruverLifes.Repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class UsuarioService implements GestorUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void registrarUsuarios(Usuario usuario) {
        // Guardar usuario
        usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorUsuario(String usuario) {
        if (usuario == null) return Optional.empty();

        String usuarioLimpio = usuario.trim().toLowerCase();

        return usuarioRepository.findAll().stream()
                .filter(u -> u.getUsuario().trim().toLowerCase().equals(usuarioLimpio))
                .findFirst();
    }

    @Override
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminarUsuario(Integer id) {

        Usuario usuario = buscarPorId(id);

        if(usuario != null){

            usuario.setEstado("INACTIVO");

            usuarioRepository.save(usuario);
        }
    }

    public Optional<Usuario> buscarPorUsuarioActivo(String usuario) {
        return usuarioRepository.findByUsuarioAndEstado(usuario, "ACTIVO");
    }

    @Override
    public List<Usuario> listarUsuarios() {

        return usuarioRepository.findByEstado("ACTIVO");
    }
}