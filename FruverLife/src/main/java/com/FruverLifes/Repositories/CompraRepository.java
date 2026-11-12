package com.FruverLifes.Repositories;

import com.FruverLifes.Model.Compra;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface CompraRepository extends MongoRepository<Compra, String> {

    List<Compra> findByProveedorId(String proveedorId);

    List<Compra> findByFechaBetween(Date inicio, Date fin);

    List<Compra> findAllByOrderByFechaDesc();
}