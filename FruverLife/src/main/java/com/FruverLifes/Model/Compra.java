package com.FruverLifes.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Document(collection = "compras")
public class Compra {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private Date fecha;
    private String usuarioId; // Referencia al _id de la Persona (Usuario)
    private String proveedorId; // Referencia al _id de la Persona (Proveedor)
    
    private List<DetalleCompra> detalle;
    private Double total;

    public Compra() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getProveedorId() { return proveedorId; }
    public void setProveedorId(String proveedorId) { this.proveedorId = proveedorId; }
    public List<DetalleCompra> getDetalle() { return detalle; }
    public void setDetalle(List<DetalleCompra> detalle) { this.detalle = detalle; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // --- SUBDOCUMENTO PARA EL DETALLE ---
    public static class DetalleCompra {
        private String productoId; // Referencia al _id del Producto
        private String nombre;
        private Double precioCompra;
        private Double cantidad;
        private Double subtotal;

        public DetalleCompra() {}

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getPrecioCompra() { return precioCompra; }
        public void setPrecioCompra(Double precioCompra) { this.precioCompra = precioCompra; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }
}
