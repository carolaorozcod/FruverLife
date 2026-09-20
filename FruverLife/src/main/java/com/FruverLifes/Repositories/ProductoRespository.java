package com.FruverLifes.Repositories;
import com.FruverLifes.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRespository extends JpaRepository<Producto, Integer>  {
    boolean existsByCodigo(int codigo);
    Producto findByCodigo(int codigo);
    List<Producto> findByCantidadLessThan(int limite);
    List<Producto> findTop10ByOrderByIdProductoDesc();
}
