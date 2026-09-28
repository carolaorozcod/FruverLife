package com.FruverLifes.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "producto")
public class Producto {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_producto")
        private Integer idProducto;

        @Column(name = "codigo", unique = true, nullable = false)
        private int codigo;

        @Column(name = "nombre", nullable = false)
        private String nombre;

        @Column(name = "precio", nullable = false)
        private Double precio;

        @Column(name = "cantidad", nullable = false)
        private int cantidad;

        @ManyToOne
        @JoinColumn(name = "id_proveedor")
        private Proveedor proveedor;

        @JsonIgnore
        @OneToMany(mappedBy = "producto")
        private List<DetalleVenta> detalles;

        @Column (name = "estado", nullable = false)
        private String estado;


        // CONSTRUCTOR
        public Producto() {
        }

        // GETTERS Y SETTERS

        public Integer getIdProducto() {
                return idProducto;
        }

        public void setIdProducto(Integer idProducto) {
                this.idProducto = idProducto;
        }

        public int getCodigo() {
                return codigo;
        }

        public void setCodigo(int codigo) {
                this.codigo = codigo;
        }

        public String getNombre() {
                return nombre;
        }

        public void setNombre(String nombre) {
                this.nombre = nombre;
        }

        public Double getPrecio() {
                return precio;
        }

        public void setPrecio(Double precio) {
                this.precio = precio;
        }

        public int getCantidad() {
                return cantidad;
        }

        public void setCantidad(int cantidad) {
                this.cantidad = cantidad;
        }

        public List<DetalleVenta> getDetalles() {
                return detalles;
        }

        public void setDetalles(List<DetalleVenta> detalles) {
                this.detalles = detalles;
        }

        public Proveedor getProveedor() {
                return proveedor;
        }

        public void setProveedor(Proveedor proveedor) {
                this.proveedor = proveedor;
        }
        public String getEstado() {
                return estado;
        }
        public void setEstado(String estado) {
                this.estado = estado;
        }
}
