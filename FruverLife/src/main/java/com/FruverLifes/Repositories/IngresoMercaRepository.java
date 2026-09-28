package com.FruverLifes.Repositories;

import com.FruverLifes.Model.IngresoMerca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngresoMercaRepository extends JpaRepository<IngresoMerca, Integer> {
    List<IngresoMerca> findAllByOrderByFechaIngresoDesc();
    List<IngresoMerca> findByProveedor_IdProveedor(int idProveedor);
}