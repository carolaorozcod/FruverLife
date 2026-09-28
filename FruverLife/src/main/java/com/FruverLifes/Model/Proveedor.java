package com.FruverLifes.Model;
import jakarta.persistence.*;

@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private int idProveedor;

    @Column(name = "NIT", nullable = false, unique = true)
    private String nit;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "numero", nullable = false)
    private String numero;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    // Constructor vacío
    public Proveedor() {
    }

    public Proveedor(String nit, String nombre, String numero, String correo) {
        this.nit = nit;
        this.nombre = nombre;
        this.numero = numero;
        this.correo = correo;
        this.estado = "ACTIVO";
    }

    // GETTERS Y SETTERS

    public int getIdProveedor() {
        return idProveedor;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {

        if(nit != null && !nit.isEmpty()){
            this.nit = nit;
        }
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {

        if(nombre != null && !nombre.isEmpty()){
            this.nombre = nombre;
        }
    }

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        if(numero != null && !numero.isEmpty()){
            this.numero = numero;
        }
    }

    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {

        if(correo != null && !correo.isEmpty()){
            this.correo = correo;
        }
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}