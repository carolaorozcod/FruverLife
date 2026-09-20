package com.FruverLifes.Repositories;
import com.FruverLifes.Model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    boolean existsByNit(String nit);
    Proveedor findByNit(String nit);
}
