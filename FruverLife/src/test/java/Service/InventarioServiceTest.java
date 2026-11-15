package com.FruverLifes.Service;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Repositories.CompraRepository;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private ProductoRespository productoRepository;
    @Mock private PersonasRepositories personasRepository;

    @InjectMocks private InventarioService service;

    // ---------- Datos de apoyo ----------
    private Persona proveedor(String estado, String... funciones) {
        Persona p = new Persona();
        p.setId("prov1");
        p.setNombre("Frutas del Valle");
        p.setFunciones(List.of(funciones));
        Persona.DatosProveedor datos = new Persona.DatosProveedor();
        datos.setNit("900123");
        datos.setEstado(estado);
        p.setDatosProveedor(datos);
        return p;
    }

    private Producto manzana(String estado) {
        Producto p = new Producto();
        p.setId("p1");
        p.setNombre("Manzana");
        p.setCantidad(10.0);
        p.setEstado(estado);
        return p;
    }

    private Compra.DetalleCompra linea(String productoId, Double cantidad, Double precio) {
        Compra.DetalleCompra d = new Compra.DetalleCompra();
        d.setProductoId(productoId);
        d.setCantidad(cantidad);
        d.setPrecioCompra(precio);
        return d;
    }

    private Compra formulario(String proveedorId, Compra.DetalleCompra... lineas) {
        Compra c = new Compra();
        c.setProveedorId(proveedorId);
        c.setDetalle(new ArrayList<>(List.of(lineas)));
        return c;
    }

    private void assertError(String mensajeEsperado, Compra form) {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarCompra(form, "u1"));
        assertEquals(mensajeEsperado, ex.getMessage());
    }

    // ---------- Validación del proveedor ----------
    @Test
    void registrarCompra_sinProveedor_lanzaError() {
        assertError("Debe seleccionar un proveedor.", formulario(null));
        assertError("Debe seleccionar un proveedor.", formulario("   "));
        verifyNoInteractions(personasRepository, compraRepository);
    }

    @Test
    void registrarCompra_proveedorNoExiste_lanzaError() {
        when(personasRepository.findById("prov1")).thenReturn(Optional.empty());

        assertError("Proveedor no encontrado", formulario("prov1"));
    }

    @Test
    void registrarCompra_personaQueNoEsProveedor_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "CLIENTE")));

        assertError("Proveedor no encontrado", formulario("prov1"));
    }

    @Test
    void registrarCompra_proveedorInactivo_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Inactivo", "PROVEEDOR")));

        assertError("El proveedor Frutas del Valle está inactivo. No se puede registrar mercancía.",
                formulario("prov1", linea("p1", 5.0, 2000.0)));
        verifyNoInteractions(compraRepository);
    }

    // ---------- Validación del detalle ----------
    @Test
    void registrarCompra_sinLineas_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));

        assertError("Debe agregar al menos un producto.", formulario("prov1"));
    }

    @Test
    void registrarCompra_lineaSinProducto_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));

        assertError("Debe seleccionar un producto.",
                formulario("prov1", linea(" ", 5.0, 2000.0)));
    }

    @Test
    void registrarCompra_cantidadCero_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));

        assertError("La cantidad debe ser mayor a cero.",
                formulario("prov1", linea("p1", 0.0, 2000.0)));
    }

    @Test
    void registrarCompra_precioNegativo_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));

        assertError("El precio de compra no es válido.",
                formulario("prov1", linea("p1", 5.0, -1.0)));
    }

    @Test
    void registrarCompra_productoNoExiste_lanzaError() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));
        when(productoRepository.findById("p1")).thenReturn(Optional.empty());

        assertError("El producto no existe.",
                formulario("prov1", linea("p1", 5.0, 2000.0)));
    }

    @Test
    void registrarCompra_productoInactivo_lanzaErrorYNoGuardaNada() {
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(manzana("INACTIVO")));

        assertError("El producto Manzana está inactivo. No se puede registrar mercancía.",
                formulario("prov1", linea("p1", 5.0, 2000.0)));
        verifyNoInteractions(compraRepository);
        verify(productoRepository, never()).save(any());
    }

    // ---------- Caso feliz ----------
    @Test
    void registrarCompra_datosValidos_guardaLaCompraYSumaElStock() {
        Producto producto = manzana("ACTIVO");   // stock inicial: 10.0
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));

        Compra resultado = service.registrarCompra(
                formulario("prov1", linea("p1", 5.0, 2000.0)), "u1");

        assertEquals(10000.0, resultado.getTotal());
        assertEquals("u1", resultado.getUsuarioId());
        assertEquals("prov1", resultado.getProveedorId());
        assertTrue(resultado.getCodigo().startsWith("COM-"));
        assertEquals(1, resultado.getDetalle().size());
        assertEquals("Manzana", resultado.getDetalle().get(0).getNombre());
        assertEquals(10000.0, resultado.getDetalle().get(0).getSubtotal());

        verify(compraRepository).save(resultado);
        assertEquals(15.0, producto.getCantidad());   // 10 + 5
        assertEquals(2000.0, producto.getPrecioCompra());   // queda el último precio de compra
        verify(productoRepository).save(producto);
    }

    @Test
    void registrarCompra_productoSinStockRegistrado_loTomaComoCero() {
        Producto producto = manzana("ACTIVO");
        producto.setCantidad(null);
        when(personasRepository.findById("prov1"))
                .thenReturn(Optional.of(proveedor("Activo", "PROVEEDOR")));
        when(productoRepository.findById("p1")).thenReturn(Optional.of(producto));

        service.registrarCompra(formulario("prov1", linea("p1", 5.0, 2000.0)), "u1");

        assertEquals(5.0, producto.getCantidad());
    }

    // ---------- Proveedores por producto ----------
    private Compra compra(String proveedorId, long fechaMs, Compra.DetalleCompra... lineas) {
        Compra c = new Compra();
        c.setProveedorId(proveedorId);
        c.setFecha(new Date(fechaMs));
        c.setDetalle(new ArrayList<>(List.of(lineas)));
        return c;
    }

    private Persona conNombre(String nombre) {
        Persona p = new Persona();
        p.setNombre(nombre);
        return p;
    }

    @Test
    void proveedoresPorProducto_agrupaPorProveedorConUltimoPrecioYCantidadTotal() {
        // Las compras llegan de la más reciente a la más antigua
        Compra reciente = compra("prov1", 3000L, linea("p1", 5.0, 3000.0));
        Compra intermedia = compra("prov2", 2000L, linea("p1", 4.0, 1800.0));
        Compra antigua = compra("prov1", 1000L, linea("p1", 10.0, 2000.0), linea("p2", 1.0, 500.0));
        when(compraRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(reciente, intermedia, antigua));
        when(personasRepository.findById("prov1")).thenReturn(Optional.of(conNombre("Frutas A")));
        when(personasRepository.findById("prov2")).thenReturn(Optional.of(conNombre("Frutas B")));

        Map<String, List<InventarioService.ProveedorProducto>> resultado = service.proveedoresPorProducto();

        List<InventarioService.ProveedorProducto> deP1 = resultado.get("p1");
        assertEquals(2, deP1.size());
        // prov1: precio y fecha de su compra más reciente; cantidad = 5 + 10
        assertEquals("Frutas A", deP1.get(0).proveedor());
        assertEquals(3000.0, deP1.get(0).precio());
        assertEquals(new Date(3000L), deP1.get(0).fecha());
        assertEquals(15.0, deP1.get(0).cantidad());
        assertEquals("Frutas B", deP1.get(1).proveedor());
        assertEquals(4.0, deP1.get(1).cantidad());
        assertEquals(1, resultado.get("p2").size());
        verify(personasRepository, times(1)).findById("prov1");   // el nombre se consulta una sola vez
    }

    @Test
    void proveedoresPorProducto_ignoraComprasIncompletasYUsaDesconocidoSiElProveedorYaNoExiste() {
        Compra sinProveedor = compra(null, 3000L, linea("p1", 1.0, 100.0));
        Compra sinDetalle = new Compra();               // detalle nulo
        sinDetalle.setProveedorId("prov1");
        sinDetalle.setFecha(new Date(2000L));
        Compra proveedorBorrado = compra("prov9", 1000L, linea("p1", 2.0, 200.0));
        when(compraRepository.findAllByOrderByFechaDesc())
                .thenReturn(List.of(sinProveedor, sinDetalle, proveedorBorrado));
        when(personasRepository.findById("prov9")).thenReturn(Optional.empty());

        Map<String, List<InventarioService.ProveedorProducto>> resultado = service.proveedoresPorProducto();

        assertEquals(1, resultado.size());
        assertEquals("Desconocido", resultado.get("p1").get(0).proveedor());
        verify(personasRepository, never()).findById("prov1");
    }

    @Test
    void proveedorPorProducto_devuelveElNombreDelProveedorMasReciente() {
        Compra reciente = compra("prov1", 3000L, linea("p1", 5.0, 3000.0));
        Compra antigua = compra("prov2", 1000L, linea("p1", 4.0, 1800.0));
        when(compraRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(reciente, antigua));
        when(personasRepository.findById("prov1")).thenReturn(Optional.of(conNombre("Frutas A")));
        when(personasRepository.findById("prov2")).thenReturn(Optional.of(conNombre("Frutas B")));

        Map<String, String> resultado = service.proveedorPorProducto();

        assertEquals("Frutas A", resultado.get("p1"));
    }
}