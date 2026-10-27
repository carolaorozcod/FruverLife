package com.FruverLifes.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "personas")
public class Persona {

    @Id
    private String id;
    private String nombre;

    @Indexed(unique = true)
    private String identificacion;

    private String telefono;
    private String correo;
    
    // Lista para guardar si es ["CLIENTE"], ["PROVEEDOR"] o ambos
    private List<String> funciones; 

    // Campos que alojarán los subdocumentos definidos abajo
    private DatosProveedor datosProveedor;
    private AccesoSistema accesoSistema;

    public Persona() {}

    // Getters y Setters de Persona
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public List<String> getFunciones() { return funciones; }
    public void setFunciones(List<String> funciones) { this.funciones = funciones; }
    public DatosProveedor getDatosProveedor() { return datosProveedor; }
    public void setDatosProveedor(DatosProveedor datosProveedor) { this.datosProveedor = datosProveedor; }
    public AccesoSistema getAccesoSistema() { return accesoSistema; }
    public void setAccesoSistema(AccesoSistema accesoSistema) { this.accesoSistema = accesoSistema; }

    // --- CLASES INTERNAS (SUBDOCUMENTOS EN MONGODB) ---

    public static class DatosProveedor {
        private String nit;
        
        public DatosProveedor() {}
        
        public String getNit() { return nit; }
        public void setNit(String nit) { this.nit = nit; }
    }

    public static class AccesoSistema {
        private Boolean habilitado;
        
        @Indexed(unique = true, sparse = true)
        private String usuario;
        
        private String contrasenaHash;
        private List<String> roles; // Ej: ["ADMINISTRADOR", "CAJERO"]
        private String estado; // ACTIVO, INACTIVO

        public AccesoSistema() {}
        
        public Boolean getHabilitado() { return habilitado; }
        public void setHabilitado(Boolean habilitado) { this.habilitado = habilitado; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getContrasenaHash() { return contrasenaHash; }
        public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }
}
