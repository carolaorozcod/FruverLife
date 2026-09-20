package com.FruverLifes.Model;

public class IngresoDTO {
    private int codigo;
    private String nit;
    private int cantidad;

    // Getters y Setters (Importantes para que Spring pueda leer los datos)
    public int getCodigo() { return codigo; }
    public void setCodigo(int codigo) { this.codigo = codigo; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}