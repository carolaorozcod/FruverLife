package com.FruverLifes.Controllers;

import com.FruverLifes.Model.IngresoDTO;
import com.FruverLifes.Service.InventarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.ProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioControllerTest {

    @Mock
    private InventarioService inventarioService;

    @Mock
    private Productoservice productoService;

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private RedirectAttributes redirectAttrs;

    @InjectMocks
    private InventarioController controller;

    private IngresoDTO ingreso;

    // Se ejecuta antes de cada test: así todos parten del mismo DTO
    @BeforeEach
    void setUp() {
        ingreso = new IngresoDTO();
        ingreso.setCodigo(1001);     // el código es numérico, no puede llevar letras
        ingreso.setNit("900123");
    }

    // ---------- Camino 1: el producto no existe ----------
    @Test
    void ingresarMercancia_productoNoExiste_muestraErrorYNoActualiza() {
        when(productoService.existeProducto(ingreso.getCodigo())).thenReturn(false);

        String vista = controller.ingresarMercancia(ingreso, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        verify(redirectAttrs).addFlashAttribute(
                "error", "El producto con código 1001 no está registrado.");
        verify(proveedorService, never()).existePorNit(any());
        verify(inventarioService, never()).procesarUnicoIngreso(any());
    }

    // ---------- Camino 2: el proveedor no existe ----------
    @Test
    void ingresarMercancia_proveedorNoExiste_muestraErrorYNoActualiza() {
        when(productoService.existeProducto(ingreso.getCodigo())).thenReturn(true);
        when(proveedorService.existePorNit(ingreso.getNit())).thenReturn(false);

        String vista = controller.ingresarMercancia(ingreso, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        verify(redirectAttrs).addFlashAttribute(
                "error", "El proveedor con NIT 900123 no existe.");
        verify(inventarioService, never()).procesarUnicoIngreso(any());
    }

    // ---------- Camino 3: todo bien ----------
    @Test
    void ingresarMercancia_datosValidos_actualizaInventario() {
        when(productoService.existeProducto(ingreso.getCodigo())).thenReturn(true);
        when(proveedorService.existePorNit(ingreso.getNit())).thenReturn(true);

        String vista = controller.ingresarMercancia(ingreso, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        verify(inventarioService).procesarUnicoIngreso(ingreso);
        verify(redirectAttrs).addFlashAttribute(
                "mensaje", "Inventario actualizado con éxito.");
    }

    // ---------- Camino 4: el service lanza una excepción ----------
    @Test
    void ingresarMercancia_siServiceFalla_muestraErrorCritico() {
        when(productoService.existeProducto(ingreso.getCodigo())).thenReturn(true);
        when(proveedorService.existePorNit(ingreso.getNit())).thenReturn(true);
        doThrow(new RuntimeException("Stock inválido"))
                .when(inventarioService).procesarUnicoIngreso(ingreso);

        String vista = controller.ingresarMercancia(ingreso, redirectAttrs);

        assertEquals("redirect:/menu/mercancia", vista);
        verify(redirectAttrs).addFlashAttribute(
                "error", "Error crítico: Stock inválido");
    }
}