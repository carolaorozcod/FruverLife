package com.FruverLifes.Service;


import com.FruverLifes.Model.Persona;

import java.util.List;
import java.util.Optional;

public interface GestorUsuarioService {
    void registrarUsuarios(Persona usuario);
    Optional<Persona> buscarPorUsuario(String usuario);
    Optional<Persona> buscarPorUsuarioActivo(String usuario);
    Persona buscarPorId(String id);
    void eliminarUsuario(String id);
    void activarUsuario(String id);
    List<Persona> listarUsuarios();
    List<Persona> listarTodosUsuarios();
}