package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InformeProveedoresService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PersonasRepositories personasRepository;

    public record FilaProveedor(String proveedorId, String nombre, String nit, String estado,
                                int numCompras, double totalComprado, double porcentaje,
                                Date ultimaCompra) {}

    public record Informe(String mes, double comprasTotales, int proveedoresActivos,
                          String proveedorTop, List<FilaProveedor> proveedores) {}

    public Informe generar(YearMonth mes) {
        List<Compra> compras = comprasDelMes(mes);

        double totalMes = compras.stream().mapToDouble(this::totalDe).sum();

        Map<String, List<Compra>> porProveedor = compras.stream()
                .filter(c -> c.getProveedorId() != null)
                .collect(Collectors.groupingBy(Compra::getProveedorId));

        List<Persona> proveedores = personasRepository.findByFuncionesContaining("PROVEEDOR");

        List<FilaProveedor> filas = new ArrayList<>();
        int activos = 0;

        for (Persona p : proveedores) {
            String estado = p.getDatosProveedor() != null ? p.getDatosProveedor().getEstado() : null;
            String nit = p.getDatosProveedor() != null ? p.getDatosProveedor().getNit() : null;
            if ("ACTIVO".equalsIgnoreCase(estado)) activos++;

            List<Compra> suyas = porProveedor.getOrDefault(p.getId(), List.of());
            double total = suyas.stream().mapToDouble(this::totalDe).sum();
            Date ultima = suyas.stream().map(Compra::getFecha).filter(Objects::nonNull)
                    .max(Date::compareTo).orElse(null);
            double porcentaje = totalMes > 0 ? total * 100.0 / totalMes : 0;

            filas.add(new FilaProveedor(p.getId(), p.getNombre(), nit, estado,
                    suyas.size(), total, porcentaje, ultima));
        }

        filas.sort(Comparator.comparingDouble((FilaProveedor f) -> f.totalComprado()).reversed());

        String top = (!filas.isEmpty() && filas.get(0).totalComprado() > 0)
                ? filas.get(0).nombre() : "—";

        return new Informe(mes.toString(), totalMes, activos, top, filas);
    }

    /** Compras de un proveedor en el mes, de la más reciente a la más antigua. */
    public List<Compra> detalle(String proveedorId, YearMonth mes) {
        return comprasDelMes(mes).stream()
                .filter(c -> proveedorId.equals(c.getProveedorId()))
                .sorted(Comparator.comparing(Compra::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<Compra> comprasDelMes(YearMonth mes) {
        // Between es exclusivo en los extremos, por eso se resta 1 ms al inicio
        Date inicio = Date.from(mes.atDay(1).atStartOfDay(ZONA).toInstant().minusMillis(1));
        Date fin = Date.from(mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant());
        return compraRepository.findByFechaBetween(inicio, fin);
    }

    private double totalDe(Compra c) {
        return c.getTotal() == null ? 0 : c.getTotal();
    }
}