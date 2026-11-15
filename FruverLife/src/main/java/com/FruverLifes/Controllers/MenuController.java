package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Compra;
import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.RegistroDesperdicio;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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
    @Autowired
    private InventarioService inventarioService;
    @Autowired
    private InformeProveedoresService informeProveedoresService;

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

        // id del producto -> todos sus proveedores (con precio y cantidad ingresada).
        // Si el producto no aparece aquí, nunca se le ha ingresado mercancía.
        Map<String, List<InventarioService.ProveedorProducto>> proveedoresPorProducto =
                inventarioService.proveedoresPorProducto();

        // AGOTADO: tiene proveedores, pero ya no le queda cantidad
        long agotados = productos.stream()
                .filter(p -> proveedoresPorProducto.containsKey(p.getId())
                        && (p.getCantidad() == null || p.getCantidad() <= 0))
                .count();

        // SIN STOCK: nunca se le ha ingresado mercancía
        long sinStock = productos.stream()
                .filter(p -> !proveedoresPorProducto.containsKey(p.getId()))
                .count();

        // Valor del inventario al costo: cantidad × último precio de compra
        double valorInventario = productos.stream()
                .filter(p -> p.getCantidad() != null && p.getPrecioCompra() != null)
                .mapToDouble(p -> p.getCantidad() * p.getPrecioCompra())
                .sum();

        model.addAttribute("productos", productos);
        model.addAttribute("proveedoresPorProducto", proveedoresPorProducto);
        model.addAttribute("agotados", agotados);
        model.addAttribute("sinStock", sinStock);
        model.addAttribute("valorInventario", valorInventario);
        model.addAttribute("contenido", "Informeme");

        return "layout/PlantillaGerente";
    }

    @GetMapping("/informe-diario")
    public String informeDiario(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        LocalDate dia = (fecha != null) ? fecha : LocalDate.now(ZoneId.of("America/Bogota"));
        List<Venta> ventas = ventaService.listarVentasDelDia(dia);
        model.addAttribute("fecha", dia);

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
        // Todos los proveedores de cada producto (con su último precio y cantidad ingresada)
        model.addAttribute("proveedoresPorProducto", inventarioService.proveedoresPorProducto());

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

        // Consultar el cliente seleccionado
        Persona clienteSesion = (Persona) session.getAttribute("clienteActual");
        Persona cliente = clienteSesion;

        if (clienteSesion != null) {

            Persona clienteActualizado =
                    gestorUsuarioService.buscarPorId(clienteSesion.getId());

            // Si MongoDB devuelve el cliente, usamos sus datos actualizados.
            // Si no, conservamos el cliente de la sesión.
            if (clienteActualizado != null) {
                cliente = clienteActualizado;
                session.setAttribute("clienteActual", clienteActualizado);
            }
        }

        model.addAttribute("cliente", cliente);

        // Recuperar carrito
        List<Venta.DetalleVenta> carrito =
                (List<Venta.DetalleVenta>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        model.addAttribute("carrito", carrito);

        // Subtotal del carrito
        double subtotal = 0;
        for (Venta.DetalleVenta d : carrito) {
            subtotal += d.getSubtotal();
        }

        // Descuento del 25 % si el cliente es usuario del sistema
        boolean esUsuarioSistema = ventaService.esUsuarioDelSistema(cliente);
        double descuentoUsuario = esUsuarioSistema
                ? ventaService.calcularDescuentoUsuario(subtotal)
                : 0;

        // Total a pagar antes de redimir puntos
        double total = subtotal - descuentoUsuario;

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("esUsuarioSistema", esUsuarioSistema);
        model.addAttribute("descuentoUsuario", descuentoUsuario);
        model.addAttribute("total", total);

        // DATOS DEL PROGRAMA DE PUNTOS
        int puntosDisponibles = 0;

        if (cliente != null) {
            puntosDisponibles = cliente.getPuntos();
        }

        // Descuento máximo del 30 % (sobre el total ya con el descuento de usuario)
        double maximoDescuento = Math.floor(total * 0.30);

        // Máximo de puntos que puede redimir
        int maximoPuntosRedimibles = 0;

        if (cliente != null && puntosDisponibles >= 10000) {
            maximoPuntosRedimibles = (int) Math.min(puntosDisponibles, maximoDescuento);
        }

        model.addAttribute("puntosDisponibles", puntosDisponibles);
        model.addAttribute("maximoDescuento", maximoDescuento);
        model.addAttribute("maximoPuntosRedimibles", maximoPuntosRedimibles);

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

    // En la sección GERENTE (junto a /informedes e /informemer):
    @GetMapping("/informeprov")
    public String informeProveedores(@RequestParam(required = false) String mes,
                                     @RequestParam(required = false) String proveedor,
                                     HttpSession session, Model model) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/auth/login";

        ZoneId zona = ZoneId.of("America/Bogota");
        YearMonth ym;
        try {
            ym = (mes == null || mes.isBlank()) ? YearMonth.now(zona) : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            ym = YearMonth.now(zona);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("mes", ym.toString());                       // ej. 2026-11
        model.addAttribute("informe", informeProveedoresService.generar(ym));

        // Detalle de un proveedor (cuando se hace clic en una fila)
        if (proveedor != null && !proveedor.isBlank()) {
            model.addAttribute("proveedorSel", proveedor);
            model.addAttribute("comprasProveedor", informeProveedoresService.detalle(proveedor, ym));
        }

        model.addAttribute("contenido", "InformeProv");
        return "layout/PlantillaGerente";
    }

}