package com.FruverLifes.Controllers;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Model.Producto;
import com.FruverLifes.Model.Venta;
import com.FruverLifes.Repositories.PersonasRepositories;
import com.FruverLifes.Repositories.ProductoRespository;
import com.FruverLifes.Service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private PersonasRepositories clienteRepository;

    @Autowired
    private ProductoRespository productoRepository;

    @Autowired
    private VentaService ventaService;

    @PostMapping("/buscarCliente")
    public String buscarCliente(@RequestParam String identificacion, HttpSession session) {

        Optional<Persona> clienteOpt = clienteRepository.findByIdentificacion(identificacion);

        if (clienteOpt.isEmpty()) {
            session.setAttribute("errorVenta", "Cliente no encontrado");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        session.setAttribute("clienteActual", clienteOpt.get());
        session.setAttribute("mensajeVenta", "Cliente encontrado correctamente");
        session.removeAttribute("errorVenta");

        return "redirect:/menu/ventas";
    }

    @PostMapping("/agregar")
    public String agregarProducto(@RequestParam String codigo,
                                  @RequestParam double cantidad,
                                  HttpSession session) {

        Producto producto = productoRepository.findByCodigo(codigo);

        if (producto == null) {
            session.setAttribute("errorVenta", "Producto no encontrado");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        if (!"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            session.setAttribute("errorVenta", "El producto " + producto.getNombre() + " está inactivo");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        if (cantidad <= 0) {
            session.setAttribute("errorVenta", "La cantidad debe ser mayor a cero");
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        List<Venta.DetalleVenta> carrito =
                (List<Venta.DetalleVenta>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        // Si el producto ya está en el carrito, se suma a esa línea
        Venta.DetalleVenta existente = null;
        for (Venta.DetalleVenta d : carrito) {
            if (d.getProductoId().equals(producto.getId())) {
                existente = d;
                break;
            }
        }

        double cantidadTotal = cantidad + (existente != null ? existente.getCantidad() : 0);

        if (cantidadTotal > producto.getCantidad()) {
            session.setAttribute("errorVenta", "Stock insuficiente para " + producto.getNombre());
            session.removeAttribute("mensajeVenta");
            return "redirect:/menu/ventas";
        }

        if (existente != null) {
            existente.setCantidad(cantidadTotal);
            existente.setSubtotal(existente.getPrecioVenta() * cantidadTotal);
        } else {
            Venta.DetalleVenta detalle = new Venta.DetalleVenta();
            detalle.setProductoId(producto.getId());
            detalle.setNombre(producto.getNombre());
            detalle.setPrecioVenta(producto.getPrecioVenta());
            detalle.setCantidad(cantidad);
            detalle.setSubtotal(producto.getPrecioVenta() * cantidad);
            carrito.add(detalle);
        }

        session.setAttribute("carrito", carrito);
        session.setAttribute("mensajeVenta", "Producto agregado al carrito");
        session.removeAttribute("errorVenta");

        return "redirect:/menu/ventas";
    }

    @GetMapping("/eliminar/{index}")
    public String eliminarProducto(@PathVariable int index, HttpSession session) {

        List<Venta.DetalleVenta> carrito =
                (List<Venta.DetalleVenta>) session.getAttribute("carrito");

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

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");
        Persona cliente = (Persona) session.getAttribute("clienteActual");
        List<Venta.DetalleVenta> carrito =
                (List<Venta.DetalleVenta>) session.getAttribute("carrito");

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

            return "redirect:/ventas/factura/" + ventaGuardada.getId();

        } catch (Exception e) {
            session.setAttribute("errorVenta", "No se pudo realizar la venta: " + e.getMessage());
            return "redirect:/menu/ventas";
        }
    }

    @GetMapping("/factura/{id}")
    public String verFactura(@PathVariable String id, Model model, HttpSession session) {

        Persona usuario = (Persona) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/auth/login";
        }

        Venta venta = ventaService.buscarPorId(id);

        if (venta == null) {
            session.setAttribute("errorVenta", "Factura no encontrada");
            return "redirect:/menu/ventas";
        }

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", venta.getDetalle());
        model.addAttribute("cliente", clienteRepository.findById(venta.getClienteId()).orElse(null));
        model.addAttribute("cajero", clienteRepository.findById(venta.getUsuarioId()).orElse(null));

        return "FacturaVenta";
    }
}