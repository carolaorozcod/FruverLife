package com.FruverLifes.Model;

import jakarta.persistence.*;
@Entity
@Table(name = "cliente")
public class Clientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private int idCliente;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "num_identificacion", nullable = false)
    private int identificacion;

    @Column(name = "numeroCe", nullable = false)
    private String numeroCe;

    @Column(nullable = false)
    private String correo;

    // Getters y setters
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdentificacion() { return identificacion; }
    public void setIdentificacion(int identificacion) { this.identificacion = identificacion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNumeroCe() { return numeroCe; }
    public void setNumeroCe(String numeroCe) { this.numeroCe = numeroCe; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}