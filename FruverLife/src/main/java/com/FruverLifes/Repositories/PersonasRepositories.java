package com.FruverLifes.Repositories;

import com.FruverLifes.Model.Persona;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PersonasRepositories extends MongoRepository<Persona, String> {

    List<Persona> findByAccesoSistemaIsNotNull();

    boolean existsByDatosProveedorNit(String nit);

    Optional<Persona> findByDatosProveedorNit(String nit);

    Optional<Persona> findByIdentificacion(String identificacion);

    Optional<Persona> findByAccesoSistemaUsuario(String usuario);

    Optional<Persona> findByAccesoSistemaUsuarioAndAccesoSistemaEstado(String usuario, String estado);

    List<Persona> findByAccesoSistemaEstado(String estado);

    List<Persona> findByFuncionesContaining(String funcion);

    Optional<Persona> findByIdAndFuncionesContaining(String id, String funcion);
}