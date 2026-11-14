
package com.FruverLifes.Repositories;

import com.FruverLifes.Model.MovimientoPuntos;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MovimientoPuntosRepository
        extends MongoRepository<MovimientoPuntos, String> {

    List<MovimientoPuntos> findByClienteIdOrderByFechaDesc(String clienteId);

    List<MovimientoPuntos> findByVentaId(String ventaId);
}
