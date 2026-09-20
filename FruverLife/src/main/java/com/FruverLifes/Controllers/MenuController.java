package com.FruverLifes.Controllers;
import com.FruverLifes.Model.*;
import com.FruverLifes.Service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

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


    //gerente
    @GetMapping("/gerente")
    public String menuB(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "inicio");

        return "layout/PlantillaGerente";
    }
    //funciones de gerente
    @GetMapping("/informedes")
    public String informeDesperdicios(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/auth/login";

        List<RegistroDesperdicio> lista = registroService.listar();
        double total = lista.stream().mapToDouble(RegistroDesperdicio::getTotal).sum();

        model.addAttribute("usuario", usuario);
        model.addAttribute("registros", lista);
        model.addAttribute("totalGeneral", total);
        model.addAttribute("contenido", "InformeDes"); // fragment Thymeleaf
        return "layout/PlantillaGerente"; // usa tu layout
    }

    @GetMapping("/informemer")
    public String verInformeMercancia(Model model) {
        // Obtener todos los productos
        List<Producto> productos = productoService.listarTodos();

        // Total de stock (opcional, si quieres un resumen)
        int totalStock = productos.stream().mapToInt(Producto::getCantidad).sum();

        // Añadir atributos para Thymeleaf
        model.addAttribute("productos", productos);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("contenido", "Informeme");

        // Retorna el fragmento de Thymeleaf (el HTML del informe)
        return "layout/PlantillaGerente"; // Este es el nombre de tu HTML con th:fragment="contenido"
    }

    @GetMapping("/informe-diario")
    public String informeDiario(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        List<Venta> ventas = ventaService.listarVentasDelDia();

        double totalDia = ventas.stream()
                .mapToDouble(Venta::getTotal)
                .sum();

        model.addAttribute("ventas", ventas);
        model.addAttribute("cantidadVentas", ventas.size());
        model.addAttribute("totalDia", totalDia);
        model.addAttribute("contenido", "InformeDiario");

        return "layout/PlantillaGerente";
    }

    @GetMapping("/usuarios")
    public String menuUsuarios(HttpSession session, Model model){
        // "usuarioLogueado" para la sesión (evita que Spring intente meter el texto del input aquí)
        model.addAttribute("usuarioLogueado", session.getAttribute("usuarioLogueado"));

        model.addAttribute("usuarioForm", new Usuario());
        model.addAttribute("usuarios", gestorUsuarioService.listarUsuarios());
        model.addAttribute("contenido", "Registrousuario");
        return "layout/PlantillaGerente";
    }
  //Administrador
    @GetMapping("/administrador")
    public String menuA(HttpSession session, Model model) {

        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("contenido", "inicio");

        return "layout/plantillaAdministrador";
    }
     //funciones de administrador
    @GetMapping("/proveedores")
    public String proveedores(Model model) {

        model.addAttribute("contenido", "proveedores");

        return "layout/plantillaAdministrador";
    }

    @GetMapping("/gestion")
    public String gestionStock(Model model) {

        model.addAttribute("contenido", "GestionStock");

        model.addAttribute(
                "productos",
                productoService.listarProductos()
        );

        return "layout/plantillaAdministrador";
    }
    @GetMapping("/mercancia")
    public String mercancia(Model model) {
        // 1. Indicar al layout qué fragmento cargar
        model.addAttribute("contenido", "mercancia");

        // 2. Enviar el DTO para el formulario (evita error si usas th:field)
        model.addAttribute("ingresoDTO", new IngresoDTO());

        // 3. Enviar la lista de productos para la tabla de la derecha
        model.addAttribute("productos", productoService.listarTodos());

        // 4. Retornar la plantilla maestra
        return "layout/plantillaAdministrador";
    }

    @GetMapping("/menu/desperdicios")
    public String desperdicios(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);

        model.addAttribute("desperdicios", registroService.listar()); // si tienes método
        model.addAttribute("total", registroService.total()); // si tienes método

        model.addAttribute("contenido", "Registrodesperdicios");

        return "layout/plantillaAdministrador";
    }
    //

    @GetMapping("/cajero")
    public String menuc(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contenido", "inicio");

        return "layout/PlantillaCajero";
    }
    //metodos de cajero

    @GetMapping("/clientes")
    public String menuClientes(HttpSession session, Model model){
        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("cliente", new Clientes()); // formulario vacío
        model.addAttribute("contenido", "Cliente"); // fragmento Thymeleaf
        return "layout/PlantillaCajero"; // layout con menú y sidebar
    }

    @GetMapping("/ventas")
    public String ventas(HttpSession session, Model model){

        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));

        Clientes cliente = (Clientes) session.getAttribute("clienteActual");
        model.addAttribute("cliente", cliente);

        List<DetalleVenta> carrito =
                (List<DetalleVenta>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        model.addAttribute("carrito", carrito);

        double total = 0;

        for (DetalleVenta d : carrito) {
            total += d.getSubtotal();
        }

        model.addAttribute("total", total);

        model.addAttribute("contenido", "Ventas");

        return "layout/PlantillaCajero";
    }
    @GetMapping("/precio")
    public String verificar(HttpSession session, Model model){

        model.addAttribute("usuario", session.getAttribute("usuarioLogueado"));

        model.addAttribute("contenido", "VerificarPrecio");

        return "layout/PlantillaCajero";
    }



}