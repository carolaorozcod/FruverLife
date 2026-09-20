package com.FruverLifes.Service;

import com.FruverLifes.Model.Usuario;
import java.util.List;
import java.util.Optional;

public interface GestorUsuarioService {
    void registrarUsuarios(Usuario usuario);
    Optional<Usuario> buscarPorUsuario(String usuario);
    Optional<Usuario> buscarPorUsuarioActivo(String usuario);
    Usuario buscarPorId(Integer id);
    void eliminarUsuario(Integer id);
    List<Usuario> listarUsuarios();
}