package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Service.PanelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
public class PanelController {

    @Autowired
    private PanelService panelService;

    @GetMapping("/menu/panel")
    public String panel(@RequestParam(required = false) String mes,
                        HttpSession session,
                        Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        YearMonth mesElegido;
        try {
            mesElegido = (mes == null || mes.isBlank()) ? YearMonth.now() : YearMonth.parse(mes);
        } catch (Exception e) {
            mesElegido = YearMonth.now();
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("meses", panelService.mesesDisponibles(12));
        model.addAttribute("mesSeleccionado", mesElegido.toString());

        model.addAttribute("kpis", panelService.kpis(mesElegido));
        model.addAttribute("ventasDia", panelService.ventasPorDia(mesElegido));
        model.addAttribute("ventasCompras", panelService.ventasVsComprasMeses(mesElegido, 6));
        model.addAttribute("topProductos", panelService.topProductos(mesElegido, 5));
        model.addAttribute("categorias", panelService.ventasPorCategoria(mesElegido));
        model.addAttribute("motivos", panelService.desperdicioPorMotivo(mesElegido));
        model.addAttribute("movimientos", panelService.ultimosMovimientos(mesElegido, 10));
        model.addAttribute("contenido", "PanelGeneral");

        return "layout/PlantillaGerente";
    }
}