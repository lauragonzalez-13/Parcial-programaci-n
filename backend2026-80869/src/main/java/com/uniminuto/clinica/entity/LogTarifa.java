package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="log_tarifa")
public class LogTarifa {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="tarifa_id",nullable=false) private TarifaServicio tarifa;
    @Column(nullable=false,length=20) private String accion;
    @Column(precision=14,scale=2) private BigDecimal valorAnterior;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal valorNuevo;
    @Column(precision=5,scale=2) private BigDecimal ivaAnterior;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal ivaNuevo;
    @Column(nullable=false,length=120) private String usuario;
    @Column(nullable=false) private LocalDateTime fecha=LocalDateTime.now();
    public Long getId(){return id;} public TarifaServicio getTarifa(){return tarifa;} public void setTarifa(TarifaServicio v){tarifa=v;}
    public String getAccion(){return accion;} public void setAccion(String v){accion=v;} public BigDecimal getValorAnterior(){return valorAnterior;} public void setValorAnterior(BigDecimal v){valorAnterior=v;}
    public BigDecimal getValorNuevo(){return valorNuevo;} public void setValorNuevo(BigDecimal v){valorNuevo=v;} public BigDecimal getIvaAnterior(){return ivaAnterior;} public void setIvaAnterior(BigDecimal v){ivaAnterior=v;}
    public BigDecimal getIvaNuevo(){return ivaNuevo;} public void setIvaNuevo(BigDecimal v){ivaNuevo=v;} public String getUsuario(){return usuario;} public void setUsuario(String v){usuario=v;} public LocalDateTime getFecha(){return fecha;}
}
