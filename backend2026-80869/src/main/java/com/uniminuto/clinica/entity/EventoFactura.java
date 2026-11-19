package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="evento_factura")
public class EventoFactura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="factura_id",nullable=false) private Factura factura;
    @Column(nullable=false,length=40) private String accion;
    @Column(length=500) private String detalle;
    @Column(nullable=false,length=120) private String usuario;
    @Column(nullable=false) private LocalDateTime fecha=LocalDateTime.now();
    public Long getId(){return id;} public Factura getFactura(){return factura;} public void setFactura(Factura v){factura=v;} public String getAccion(){return accion;} public void setAccion(String v){accion=v;}
    public String getDetalle(){return detalle;} public void setDetalle(String v){detalle=v;} public String getUsuario(){return usuario;} public void setUsuario(String v){usuario=v;} public LocalDateTime getFecha(){return fecha;}
}
