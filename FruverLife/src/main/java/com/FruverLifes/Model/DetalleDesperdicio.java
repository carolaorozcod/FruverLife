package com.FruverLifes.Model;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "detalle_desperdicio")
public class DetalleDesperdicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;
    @Column(name = "codigo_producto")
    private int codigoProducto;
    @Column(name = "nombre_producto")
    private String nombreProducto;
    private int cantidad;
    private double subtotal;
    @ManyToOne
    @JoinColumn(name = "id_registro")
    private RegistroDesperdicio registro;
    public DetalleDesperdicio() {
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }
    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }
    public int getCodigoProducto() {
        return codigoProducto;
    }
    public void setCodigoProducto(int codigoProducto) {
        this.codigoProducto = codigoProducto;
    }
    public String getNombreProducto() {
        return nombreProducto;
    }
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
    public double getSubtotal() {
        return subtotal;
    }
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
    public RegistroDesperdicio getRegistro() {
        return registro;
    }
    public void setRegistro(RegistroDesperdicio registro) {
        this.registro = registro;
    }
}
