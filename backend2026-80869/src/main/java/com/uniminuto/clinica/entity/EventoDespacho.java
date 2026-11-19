package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="evento_despacho")
public class EventoDespacho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="despacho_id",nullable=false) private Despacho despacho;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private EstadoDespacho estado;
    @Column(nullable=false,length=120) private String usuarioResponsable;
    @Column(nullable=false) private LocalDateTime fecha=LocalDateTime.now();
    @Column(length=500) private String observacion;
    public Long getId(){return id;} public Despacho getDespacho(){return despacho;} public void setDespacho(Despacho v){despacho=v;}
    public EstadoDespacho getEstado(){return estado;} public void setEstado(EstadoDespacho v){estado=v;}
    public String getUsuarioResponsable(){return usuarioResponsable;} public void setUsuarioResponsable(String v){usuarioResponsable=v;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime v){fecha=v;}
    public String getObservacion(){return observacion;} public void setObservacion(String v){observacion=v;}
}
