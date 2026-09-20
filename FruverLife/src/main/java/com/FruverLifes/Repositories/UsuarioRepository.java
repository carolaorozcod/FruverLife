package com.FruverLifes.Repositories;
import com.FruverLifes.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

        Optional<Usuario> findByUsuario(String usuario);
        Optional<Usuario> findByUsuarioAndEstado(String usuario, String estado);
        List<Usuario> findByEstado(String estado);
}
