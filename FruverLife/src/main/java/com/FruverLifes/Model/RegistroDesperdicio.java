package com.FruverLifes.Model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "registro_desperdicio")
public class RegistroDesperdicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;
    private LocalDate fecha;
    private String usuario;
    private double total;
    @OneToMany(
            mappedBy = "registro",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<DetalleDesperdicio> detalles;

    public RegistroDesperdicio() {
    }

    public Integer getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Integer idRegistro) {
        this.idRegistro = idRegistro;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
        public String getUsuario () {
            return usuario;
        }
        public void setUsuario (String usuario){
            this.usuario = usuario;
        }
        public double getTotal() {
            return total;
        }
        public void setTotal(double total){
            this.total = total;
        }
        public List<DetalleDesperdicio> getDetalles () {
            return detalles;
        }
        public void setDetalles(List<DetalleDesperdicio> detalles) {
            this.detalles = detalles;
        }
}