package com.FruverLifes.Repositories;
import com.FruverLifes.Model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

}
