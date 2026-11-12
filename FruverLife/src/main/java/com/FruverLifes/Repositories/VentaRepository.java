package com.FruverLifes.Repositories;

import com.FruverLifes.Model.Venta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface VentaRepository extends MongoRepository<Venta, String> {

    List<Venta> findByFechaBetween(Date inicio, Date fin);

    List<Venta> findByClienteId(String clienteId);
}