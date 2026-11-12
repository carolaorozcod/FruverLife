package com.FruverLifes.Repositories;

import com.FruverLifes.Model.Producto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductoRespository extends MongoRepository<Producto, String> {

    boolean existsByCodigo(String codigo);

    Producto findByCodigo(String codigo);

    List<Producto> findByCantidadLessThanAndEstado(Double limite, String estado);

    List<Producto> findTop10ByOrderByIdDesc();
}