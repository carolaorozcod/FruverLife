package com.FruverLifes.Repositories;
import com.FruverLifes.Model.Clientes;
import com.FruverLifes.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Clientes, Integer> {
    Clientes findByIdentificacion(int identificacion);
}
