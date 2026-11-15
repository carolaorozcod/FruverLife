package com.FruverLifes.Service;

import com.FruverLifes.Model.*;
import com.FruverLifes.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PanelService {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private CompraRepository compraRepository;
    @Autowired private ProductoRespository productoRepository;
    @Autowired private PersonasRepositories personasRepository;
    @Autowired private RegistroDesperdicioService desperdicioService;
    @Autowired private Productoservice productoService;

    private static final ZoneId ZONA = ZoneId.systemDefault();

    // ---------- utilidades ----------
    private LocalDate dia(Date d) {
        return d == null ? null : d.toInstant().atZone(ZONA).toLocalDate();
    }

    private double num(Double d) {
        return d == null ? 0 : d;
    }

    private boolean esDelMes(Date d, YearMonth mes) {
        LocalDate f = dia(d);
        return f != null && YearMonth.from(f).equals(mes);
    }

    // ---------- selector de meses ----------
    public Map<String, String> mesesDisponibles(int cantidad) {
        Map<String, String> meses = new LinkedHashMap<>();
        YearMonth actual = YearMonth.now();
        for (int i = 0; i < cantidad; i++) {
            YearMonth ym = actual.minusMonths(i);
            String nombre = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
            nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
            meses.put(ym.toString(), nombre + " " + ym.getYear());   // "2026-11" -> "Noviembre 2026"
        }
        return meses;
    }
    // ---------- tarjetas ----------
    public Map<String, Object> kpis(YearMonth mes) {
        LocalDate hoy = LocalDate.now();

        List<Venta> ventas = ventaRepository.findAll();
        List<Compra> compras = compraRepository.findAll();
        List<RegistroDesperdicio> desperdicios = desperdicioService.listar();

        double ventasHoy = ventas.stream()
                .filter(v -> hoy.equals(dia(v.getFecha())))
                .mapToDouble(v -> num(v.getTotal())).sum();

        double ventasMes = ventas.stream()
                .filter(v -> esDelMes(v.getFecha(), mes))
                .mapToDouble(v -> num(v.getTotal())).sum();

        double comprasMes = compras.stream()
                .filter(c -> esDelMes(c.getFecha(), mes))
                .mapToDouble(c -> num(c.getTotal())).sum();

        double perdidaMes = desperdicios.stream()
                .filter(r -> esDelMes(r.getFecha(), mes))
                .mapToDouble(r -> num(r.getTotal())).sum();

        // costo de lo vendido en el mes (último precio de compra de cada producto)
        Map<String, Double> costos = productoRepository.findAll().stream()
                .collect(Collectors.toMap(Producto::getId, p -> num(p.getPrecioCompra()), (a, b) -> a));

        double costoVendidoMes = ventas.stream()
                .filter(v -> esDelMes(v.getFecha(), mes) && v.getDetalle() != null)
                .flatMap(v -> v.getDetalle().stream())
                .mapToDouble(d -> costos.getOrDefault(d.getProductoId(), 0.0) * num(d.getCantidad()))
                .sum();

        long clientes = personasRepository.findByFuncionesContaining("CLIENTE").size();

        long proveedoresActivos = personasRepository.findByFuncionesContaining("PROVEEDOR").stream()
                .filter(p -> p.getDatosProveedor() != null
                        && "ACTIVO".equalsIgnoreCase(p.getDatosProveedor().getEstado()))
                .count();

        Map<String, Object> k = new LinkedHashMap<>();
        k.put("esMesActual", mes.equals(YearMonth.now()));
        k.put("ventasHoy", ventasHoy);
        k.put("ventasMes", ventasMes);
        k.put("comprasMes", comprasMes);
        k.put("desperdicioMes", perdidaMes);
        k.put("utilidadMes", ventasMes - costoVendidoMes - perdidaMes);
        k.put("flujoCajaMes", ventasMes - comprasMes - perdidaMes);
        k.put("clientes", clientes);
        k.put("proveedoresActivos", proveedoresActivos);
        k.put("stockBajo", productoService.listarProductosAgotandose().size());
        return k;
    }

    // ---------- ventas por día del mes ----------
    public Map<String, Double> ventasPorDia(YearMonth mes) {
        int ultimoDia = mes.equals(YearMonth.now())
                ? LocalDate.now().getDayOfMonth()
                : mes.lengthOfMonth();

        Map<Integer, Double> porDia = new LinkedHashMap<>();
        for (int d = 1; d <= ultimoDia; d++) {
            porDia.put(d, 0.0);
        }
        for (Venta v : ventaRepository.findAll()) {
            LocalDate f = dia(v.getFecha());
            if (f != null && YearMonth.from(f).equals(mes) && porDia.containsKey(f.getDayOfMonth())) {
                porDia.merge(f.getDayOfMonth(), num(v.getTotal()), Double::sum);
            }
        }
        Map<String, Double> salida = new LinkedHashMap<>();
        porDia.forEach((d, total) -> salida.put(String.format("%02d", d), total));
        return salida;
    }

    // ---------- ventas vs compras (N meses terminando en el mes elegido) ----------
    public Map<String, Map<String, Double>> ventasVsComprasMeses(YearMonth hasta, int meses) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        Map<YearMonth, double[]> acum = new LinkedHashMap<>();
        for (int i = meses - 1; i >= 0; i--) {
            acum.put(hasta.minusMonths(i), new double[]{0, 0});
        }
        for (Venta v : ventaRepository.findAll()) {
            LocalDate f = dia(v.getFecha());
            if (f != null && acum.containsKey(YearMonth.from(f))) {
                acum.get(YearMonth.from(f))[0] += num(v.getTotal());
            }
        }
        for (Compra c : compraRepository.findAll()) {
            LocalDate f = dia(c.getFecha());
            if (f != null && acum.containsKey(YearMonth.from(f))) {
                acum.get(YearMonth.from(f))[1] += num(c.getTotal());
            }
        }
        Map<String, Double> ventas = new LinkedHashMap<>();
        Map<String, Double> compras = new LinkedHashMap<>();
        acum.forEach((m, a) -> {
            ventas.put(m.format(fmt), a[0]);
            compras.put(m.format(fmt), a[1]);
        });
        Map<String, Map<String, Double>> salida = new LinkedHashMap<>();
        salida.put("ventas", ventas);
        salida.put("compras", compras);
        return salida;
    }

    // ---------- top productos vendidos del mes ----------
    public Map<String, Double> topProductos(YearMonth mes, int limite) {
        Map<String, Double> cantidades = new HashMap<>();
        for (Venta v : ventaRepository.findAll()) {
            if (!esDelMes(v.getFecha(), mes) || v.getDetalle() == null) continue;
            for (Venta.DetalleVenta d : v.getDetalle()) {
                if (d.getNombre() != null) {
                    cantidades.merge(d.getNombre(), num(d.getCantidad()), Double::sum);
                }
            }
        }
        return cantidades.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limite)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    // ---------- ventas por categoría del mes (en pesos) ----------
    public Map<String, Double> ventasPorCategoria(YearMonth mes) {
        Map<String, String> categorias = new HashMap<>();
        for (Producto p : productoRepository.findAll()) {
            String cat = (p.getCategoria() == null || p.getCategoria().isBlank())
                    ? "Sin categoría" : p.getCategoria().trim();
            categorias.put(p.getId(), cat);
        }

        Map<String, Double> porCategoria = new LinkedHashMap<>();
        for (Venta v : ventaRepository.findAll()) {
            if (!esDelMes(v.getFecha(), mes) || v.getDetalle() == null) continue;
            for (Venta.DetalleVenta d : v.getDetalle()) {
                String cat = categorias.getOrDefault(d.getProductoId(), "Sin categoría");
                porCategoria.merge(cat, num(d.getSubtotal()), Double::sum);
            }
        }
        return porCategoria;
    }

    // ---------- desperdicio por motivo del mes ----------
    public Map<String, Double> desperdicioPorMotivo(YearMonth mes) {
        Map<String, Double> porMotivo = new LinkedHashMap<>();
        for (RegistroDesperdicio r : desperdicioService.listar()) {
            if (!esDelMes(r.getFecha(), mes)) continue;
            String motivo = (r.getMotivo() == null || r.getMotivo().isBlank())
                    ? "Sin motivo" : r.getMotivo().trim();
            porMotivo.merge(motivo, num(r.getTotal()), Double::sum);
        }
        return porMotivo;
    }

    // ---------- últimos movimientos del mes ----------
    public static class Movimiento {
        private final String tipo;
        private final Date fecha;
        private final String codigo;
        private final double monto;

        public Movimiento(String tipo, Date fecha, String codigo, double monto) {
            this.tipo = tipo; this.fecha = fecha; this.codigo = codigo; this.monto = monto;
        }
        public String getTipo() { return tipo; }
        public Date getFecha() { return fecha; }
        public String getCodigo() { return codigo; }
        public double getMonto() { return monto; }
    }

    public List<Movimiento> ultimosMovimientos(YearMonth mes, int limite) {
        List<Movimiento> lista = new ArrayList<>();

        for (Venta v : ventaRepository.findAll()) {
            if (esDelMes(v.getFecha(), mes))
                lista.add(new Movimiento("Venta", v.getFecha(), v.getCodigo(), num(v.getTotal())));
        }
        for (Compra c : compraRepository.findAll()) {
            if (esDelMes(c.getFecha(), mes))
                lista.add(new Movimiento("Compra", c.getFecha(), c.getCodigo(), num(c.getTotal())));
        }
        for (RegistroDesperdicio r : desperdicioService.listar()) {
            if (esDelMes(r.getFecha(), mes))
                lista.add(new Movimiento("Desperdicio", r.getFecha(), r.getCodigo(), num(r.getTotal())));
        }

        return lista.stream()
                .sorted(Comparator.comparing(Movimiento::getFecha).reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }
}