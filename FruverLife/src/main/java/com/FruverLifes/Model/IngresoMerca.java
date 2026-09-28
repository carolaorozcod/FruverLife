package com.FruverLifes.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingreso_mercancia")
public class IngresoMerca {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_ingreso")
        private Integer idIngreso;

        @ManyToOne
        @JoinColumn(name = "id_proveedor", nullable = false)
        private Proveedor proveedor;

        @ManyToOne
        @JoinColumn(name = "id_producto", nullable = false)
        private Producto producto;

        @Column(name = "cantidad", nullable = false)
        private int cantidad;

        @Column(name = "fecha_ingreso", nullable = false)
        private LocalDateTime fechaIngreso;

        public IngresoMerca() {
        }

        public IngresoMerca(Proveedor proveedor, Producto producto, int cantidad, LocalDateTime fechaIngreso) {
            this.proveedor = proveedor;
            this.producto = producto;
            this.cantidad = cantidad;
            this.fechaIngreso = fechaIngreso;
        }

        // GETTERS Y SETTERS

        public Integer getIdIngreso() {
            return idIngreso;
        }

        public Proveedor getProveedor() {
            return proveedor;
        }

        public void setProveedor(Proveedor proveedor) {
            this.proveedor = proveedor;
        }

        public Producto getProducto() {
            return producto;
        }

        public void setProducto(Producto producto) {
            this.producto = producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }

        public LocalDateTime getFechaIngreso() {
            return fechaIngreso;
        }

        public void setFechaIngreso(LocalDateTime fechaIngreso) {
            this.fechaIngreso = fechaIngreso;
        }
}

