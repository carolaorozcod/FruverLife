package com.FruverLifes.Controllers;

import com.FruverLifes.Model.*;
import com.FruverLifes.Repositories.ClienteRepository;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRespository productoRepository;

    @Autowired
    private VentaService ventaService;

    @PostMapping("/buscarCliente")
    public String buscarCliente(@RequestParam int identificacion, HttpSession session) {

        Clientes cliente = clienteRepository.findByIdentificacion(identificacion);

        if (cliente == null) {
            session.setAttribute("errorVenta", "Cliente no encontrado");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        session.setAttribute("clienteActual", cliente);
        session.setAttribute("mensajeVenta", "Cliente encontrado correctamente");
        session.removeAttribute("errorVenta");

        return "redirect:/menu/ventas";
    }

    @PostMapping("/agregar")
    public String agregarProducto(@RequestParam int codigo,
                                  @RequestParam int cantidad,
                                  HttpSession session) {

        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            session.setAttribute("errorVenta", "Producto no encontrado");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        if (cantidad <= 0) {
            session.setAttribute("errorVenta", "La cantidad debe ser mayor a cero");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        if (cantidad > producto.getCantidad()) {
            session.setAttribute("errorVenta", "Stock insuficiente para " + producto.getNombre());
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecio(producto.getPrecio());
        detalle.setSubtotal(producto.getPrecio() * cantidad);

        List<DetalleVenta> carrito =
                (List<DetalleVenta>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        carrito.add(detalle);

        session.setAttribute("carrito", carrito);
        session.setAttribute("mensajeVenta", "Producto agregado al carrito");
        session.removeAttribute("errorVenta");

        return "redirect:/menu/ventas";
    }

    @GetMapping("/eliminar/{index}")
    public String eliminarProducto(@PathVariable int index, HttpSession session) {

        List<DetalleVenta> carrito =
                (List<DetalleVenta>) session.getAttribute("carrito");

        if (carrito != null && index >= 0 && index < carrito.size()) {
            carrito.remove(index);
            session.setAttribute("mensajeVenta", "Producto eliminado del carrito");
            session.removeAttribute("errorVenta");
        }

        session.setAttribute("carrito", carrito);

        return "redirect:/menu/ventas";
    }

    @PostMapping("/finalizar")
    public String finalizarVenta(HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        Clientes cliente = (Clientes) session.getAttribute("clienteActual");
        List<DetalleVenta> carrito = (List<DetalleVenta>) session.getAttribute("carrito");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        if (cliente == null) {
            session.setAttribute("errorVenta", "Debe buscar un cliente antes de finalizar la venta");
            return "redirect:/menu/ventas";
        }

        if (carrito == null || carrito.isEmpty()) {
            session.setAttribute("errorVenta", "Debe agregar productos al carrito");
            return "redirect:/menu/ventas";
        }

        try {
            Venta ventaGuardada = ventaService.finalizarVenta(usuario, cliente, carrito);

            session.removeAttribute("carrito");
            session.removeAttribute("clienteActual");
            session.removeAttribute("errorVenta");

            return "redirect:/ventas/factura/" + ventaGuardada.getIdVenta();

        } catch (Exception e) {
            session.setAttribute("errorVenta", "No se pudo realizar la venta: " + e.getMessage());
            return "redirect:/menu/ventas";
        }
    }

    @GetMapping("/factura/{id}")
    public String verFactura(@PathVariable int id, Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        Venta venta = ventaService.buscarPorId(id);

        if (venta == null) {
            session.setAttribute("errorVenta", "Factura no encontrada");
            return "redirect:/menu/ventas";
        }

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", venta.getDetalles());

        return "FacturaVenta";
    }
}