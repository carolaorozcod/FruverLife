package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.PanelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanelControllerTest {

    @Mock private PanelService panelService;
    @InjectMocks private PanelController controller;

    private MockHttpSession session;
    private ConcurrentModel model;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        model = new ConcurrentModel();
    }

    private void iniciarSesion() {
        session.setAttribute("usuarioLogueado", new Persona());
    }

    @Test
    void panel_sinSesion_redirigeAlLogin() {
        assertEquals("redirect:/auth/login", controller.panel("2026-10", session, model));
        verifyNoInteractions(panelService);
    }

    @Test
    void panel_conMesValido_cargaTodosLosDatosDeEseMes() {
        iniciarSesion();
        YearMonth mes = YearMonth.of(2026, 10);
        Map<String, Object> kpis = Map.of("ventasMes", 100.0);
        Map<String, Double> ventasDia = Map.of("01", 5.0);
        when(panelService.kpis(mes)).thenReturn(kpis);
        when(panelService.ventasPorDia(mes)).thenReturn(ventasDia);
        when(panelService.ultimosMovimientos(mes, 10)).thenReturn(List.of());

        String vista = controller.panel("2026-10", session, model);

        assertEquals("layout/PlantillaGerente", vista);
        assertEquals("PanelGeneral", model.getAttribute("contenido"));
        assertEquals("2026-10", model.getAttribute("mesSeleccionado"));
        assertSame(kpis, model.getAttribute("kpis"));
        assertSame(ventasDia, model.getAttribute("ventasDia"));
        // el panel pide a cada gráfico los datos del mes elegido
        verify(panelService).mesesDisponibles(12);
        verify(panelService).ventasVsComprasMeses(mes, 6);
        verify(panelService).topProductos(mes, 5);
        verify(panelService).ventasPorCategoria(mes);
        verify(panelService).desperdicioPorMotivo(mes);
    }

    // Se repite con: sin mes, mes vacío, solo espacios y textos que no son un mes
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "2026-13"})
    void panel_conMesAusenteOInvalido_usaElMesActual(String mes) {
        iniciarSesion();

        controller.panel(mes, session, model);

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(panelService).kpis(captor.capture());
        assertEquals(YearMonth.now(), captor.getValue());
        assertEquals(captor.getValue().toString(), model.getAttribute("mesSeleccionado"));
    }
}
