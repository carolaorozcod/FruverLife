package com.FruverLifes.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Document(collection = "registrosDesperdicio")
public class RegistroDesperdicio {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private Date fecha;
    private String usuarioId; // Referencia al _id de la Persona (Usuario que reporta)
    private String motivo;

    private List<DetalleDesperdicio> detalle;
    private Double total;

    public RegistroDesperdicio() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public List<DetalleDesperdicio> getDetalle() { return detalle; }
    public void setDetalle(List<DetalleDesperdicio> detalle) { this.detalle = detalle; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // --- SUBDOCUMENTO PARA EL DETALLE ---
    public static class DetalleDesperdicio {
        private String productoId; // Referencia al _id del Producto
        private String nombre;
        private Double cantidad;
        private Double costoUnitario;
        private Double subtotal;

        public DetalleDesperdicio() {}

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public Double getCostoUnitario() { return costoUnitario; }
        public void setCostoUnitario(Double costoUnitario) { this.costoUnitario = costoUnitario; }
        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }
}
