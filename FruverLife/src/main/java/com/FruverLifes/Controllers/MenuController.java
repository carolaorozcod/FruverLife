package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Service.ClienteService;
import com.FruverLifes.Service.GestorUsuarioService;
import com.FruverLifes.Service.Productoservice;
import com.FruverLifes.Service.ProveedorService;
import com.FruverLifes.Service.RegistroDesperdicioService;
import com.FruverLifes.Service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private Productoservice productoService;
    @Autowired
    private RegistroDesperdicioService registroService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private GestorUsuarioService gestorUsuarioService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private ProveedorService proveedorService;

    // ---------- GERENTE ----------
    @GetMapping("/gerente")
    public String menuB(HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "inicio");

        return "layout/PlantillaGerente";
    }
    @GetMapping("/informedes")
    public String informeDesperdicios(HttpSession session, Model model) {
        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/auth/login";

        List<RegistroDesperdicio> lista = registroService.listar();

        // id del usuario -> nombre, para mostrar el responsable
        Map<String, String> responsables = new HashMap<>();
        for (RegistroDesperdicio r : lista) {
            String uid = r.getUsuarioId();
            if (uid != null && !responsables.containsKey(uid)) {
                Persona p = gestorUsuarioService.buscarPorId(uid);
                responsables.put(uid, p != null ? p.getNombre() : "Desconocido");
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("registros", lista);
        model.addAttribute("responsables", responsables);
        model.addAttribute("totalGeneral", registroService.total());
        model.addAttribute("contenido", "InformeDes");
        return "layout/PlantillaGerente";
    }

    @GetMapping("/informemer")
    public String verInformeMercancia(Model model) {
        List<Producto> productos = productoService.listarTodos();

        double totalStock = productos.stream()
                .filter(p -> p.getCantidad() != null)
                .mapToDouble(Producto::getCantidad)
                .sum();

        model.addAttribute("productos", productos);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("contenido", "Informeme");

        return "layout/PlantillaGerente";
    }

    @GetMapping("/informe-diario")
    public String informeDiario(HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        List<Venta> ventas = ventaService.listarVentasDelDia();

        double totalDia = ventas.stream()
                .filter(v -> v.getTotal() != null)
                .mapToDouble(Venta::getTotal)
                .sum();

        // id -> nombre, para mostrar cliente y cajero
        Map<String, String> clientes = new HashMap<>();
        Map<String, String> cajeros = new HashMap<>();

        for (Venta v : ventas) {
            if (v.getClienteId() != null && !clientes.containsKey(v.getClienteId())) {
                Persona c = clienteService.buscarPorId(v.getClienteId());
                clientes.put(v.getClienteId(), c != null ? c.getNombre() : "Desconocido");
            }
            if (v.getUsuarioId() != null && !cajeros.containsKey(v.getUsuarioId())) {
                Persona u = gestorUsuarioService.buscarPorId(v.getUsuarioId());
                cajeros.put(v.getUsuarioId(), u != null ? u.getNombre() : "Desconocido");
            }
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("clientes", clientes);
        model.addAttribute("cajeros", cajeros);
        model.addAttribute("cantidadVentas", ventas.size());
        model.addAttribute("totalDia", totalDia);
        model.addAttribute("contenido", "InformeDiario");

        return "layout/PlantillaGerente";
    }

    @GetMapping("/usuarios")
    public String menuUsuarios(HttpSession session, Model model) {
        model.addAttribute("usuarioLogueado", session.getAttribute("usuarioLogueado"));

        Persona usuarioForm = new Persona();
        Persona.AccesoSistema acceso = new Persona.AccesoSistema();
        acceso.setEstado("ACTIVO");
        acceso.setHabilitado(true);
        usuarioForm.setAccesoSistema(acceso);

        model.addAttribute("usuarioForm", usuarioForm);
        model.addAttribute("usuarios", gestorUsuarioService.listarTodosUsuarios());
        model.addAttribute("contenido", "Registrousuario");
        return "layout/PlantillaGerente";
    }

    // ---------- ADMINISTRADOR ----------
    @GetMapping("/administrador")
    public String menuA(HttpSession session, Model model) {

        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("contenido", "inicio");

        return "layout/plantillaAdministrador";
    }

    @GetMapping("/proveedores")
    public String proveedores(Model model) {

        model.addAttribute("contenido", "proveedores");

        return "layout/plantillaAdministrador";
    }

    @GetMapping("/gestion")
    public String gestionStock(Model model) {

        model.addAttribute("contenido", "GestionStock");
        model.addAttribute("productos", productoService.listarProductos());

        return "layout/plantillaAdministrador";
    }

    @GetMapping("/mercancia")
    public String mercancia(Model model) {

        // Formulario vacío con una línea de detalle ya creada
        Compra compra = new Compra();
        compra.setDetalle(new ArrayList<>(List.of(new Compra.DetalleCompra())));

        // Solo proveedores activos
        List<Persona> proveedoresActivos = proveedorService.listar().stream()
                .filter(p -> p.getDatosProveedor() != null
                        && "ACTIVO".equalsIgnoreCase(p.getDatosProveedor().getEstado()))
                .toList();

        model.addAttribute("contenido", "Mercancia");
        model.addAttribute("compra", compra);
        model.addAttribute("proveedores", proveedoresActivos);
        model.addAttribute("productos", productoService.listarTodos());

        return "layout/plantillaAdministrador";
    }

    // (/menu/desperdicios lo atiende RegistroDesperdicioController)

    // ---------- CAJERO ----------
    @GetMapping("/cajero")
    public String menuc(HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "inicio");

        return "layout/PlantillaCajero";
    }

    @GetMapping("/clientes")
    public String menuClientes(HttpSession session, Model model) {
        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("cliente", new Persona());
        model.addAttribute("contenido", "Cliente");
        return "layout/PlantillaCajero";
    }

    @GetMapping("/ventas")
    public String ventas(HttpSession session, Model model) {

        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));

        Persona cliente = (Persona) session.getAttribute("clienteActual");
        model.addAttribute("cliente", cliente);

        List<Venta.DetalleVenta> carrito =
                (List<Venta.DetalleVenta>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        model.addAttribute("carrito", carrito);

        double total = 0;

        for (Venta.DetalleVenta d : carrito) {
            total += d.getSubtotal();
        }

        model.addAttribute("total", total);
        model.addAttribute("contenido", "Ventas");

        return "layout/PlantillaCajero";
    }

    @GetMapping("/precio")
    public String verificar(HttpSession session, Model model) {

        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));
        model.addAttribute("contenido", "VerificarPrecio");

        return "layout/PlantillaCajero";
    }

    @GetMapping("/catalogo")
    public String catalogo(HttpSession session, Model model) {

        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));
        model.addAttribute("productos", productoService.listarTodos().stream()
                .filter(p -> "ACTIVO".equalsIgnoreCase(p.getEstado()))
                .toList());
        model.addAttribute("contenido", "CatalogoProductos");

        return "layout/PlantillaCajero";
    }
}