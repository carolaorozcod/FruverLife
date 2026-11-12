package com.FruverLifes.Repositories;

import com.FruverLifes.Model.RegistroDesperdicio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface RegistroDesperdicioRepository extends MongoRepository<RegistroDesperdicio, String> {

    List<RegistroDesperdicio> findAllByOrderByFechaDesc();

    List<RegistroDesperdicio> findByFechaBetween(Date inicio, Date fin);
}