package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="despacho_medicamento", uniqueConstraints=@UniqueConstraint(name="uk_despacho_formula", columnNames="formula_medica_id"))
public class Despacho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(name="formula_medica_id", nullable=false, unique=true) private FormulaMedica formula;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private EstadoDespacho estado=EstadoDespacho.PENDIENTE_DESPACHO;
    @Column(nullable=false) private LocalDateTime creadoEn=LocalDateTime.now();
    private LocalDateTime actualizadoEn;
    private LocalDateTime entregadoEn;
    @Column(length=120) private String usuarioResponsable;
    @Column(length=500) private String observacion;
    public Long getId(){return id;} public FormulaMedica getFormula(){return formula;} public void setFormula(FormulaMedica v){formula=v;}
    public EstadoDespacho getEstado(){return estado;} public void setEstado(EstadoDespacho v){estado=v;}
    public LocalDateTime getCreadoEn(){return creadoEn;} public void setCreadoEn(LocalDateTime v){creadoEn=v;}
    public LocalDateTime getActualizadoEn(){return actualizadoEn;} public void setActualizadoEn(LocalDateTime v){actualizadoEn=v;}
    public LocalDateTime getEntregadoEn(){return entregadoEn;} public void setEntregadoEn(LocalDateTime v){entregadoEn=v;}
    public String getUsuarioResponsable(){return usuarioResponsable;} public void setUsuarioResponsable(String v){usuarioResponsable=v;}
    public String getObservacion(){return observacion;} public void setObservacion(String v){observacion=v;}
}
