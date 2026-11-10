package com.FruverLifes.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Document(collection = "ventas")
public class Venta {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    @Indexed(unique = true)
    private String factura;

    private Date fecha;
    private String usuarioId; // Referencia al _id de la Persona (Cajero/Usuario)
    private String clienteId; // Referencia al _id de la Persona (Cliente)

    private List<DetalleVenta> detalle;
    private Double total;

    public Venta() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getFactura() { return factura; }
    public void setFactura(String factura) { this.factura = factura; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public List<DetalleVenta> getDetalle() { return detalle; }
    public void setDetalle(List<DetalleVenta> detalle) { this.detalle = detalle; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // --- SUBDOCUMENTO PARA EL DETALLE ---
    public static class DetalleVenta {
        private String productoId; // Referencia al _id del Producto
        private String nombre;
        private Double precioVenta;
        private Double cantidad;
        private Double subtotal;

        public DetalleVenta() {}

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getPrecioVenta() { return precioVenta; }
        public void setPrecioVenta(Double precioVenta) { this.precioVenta = precioVenta; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }
}
