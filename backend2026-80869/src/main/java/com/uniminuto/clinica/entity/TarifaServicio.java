package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="tarifa_servicio")
public class TarifaServicio {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=40) private String codigo;
    @Column(nullable=false,length=140) private String nombre;
    @Column(length=500) private String descripcion;
    @Column(nullable=false,length=30) private String tipoServicio="CONSULTA";
    @ManyToOne @JoinColumn(name="especialidad_id") private EspecialidadMedica especialidad;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal valor=BigDecimal.ZERO;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal ivaPorcentaje=BigDecimal.ZERO;
    @Column(nullable=false) private boolean activa=true;
    @Column(nullable=false) private LocalDateTime fechaActualizacion=LocalDateTime.now();
    @PreUpdate void actualizarFecha(){fechaActualizacion=LocalDateTime.now();}
    public Long getId(){return id;} public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;} public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getTipoServicio(){return tipoServicio;} public void setTipoServicio(String v){tipoServicio=v;} public EspecialidadMedica getEspecialidad(){return especialidad;} public void setEspecialidad(EspecialidadMedica v){especialidad=v;}
    public BigDecimal getValor(){return valor;} public void setValor(BigDecimal v){valor=v;} public BigDecimal getIvaPorcentaje(){return ivaPorcentaje;} public void setIvaPorcentaje(BigDecimal v){ivaPorcentaje=v;}
    public boolean isActiva(){return activa;} public void setActiva(boolean v){activa=v;} public LocalDateTime getFechaActualizacion(){return fechaActualizacion;}
}
