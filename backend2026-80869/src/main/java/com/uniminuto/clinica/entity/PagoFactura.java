package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="pago_factura")
public class PagoFactura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="factura_id",nullable=false) private Factura factura;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal valor;
    @Column(nullable=false,length=30) private String medio;
    @Column(length=100) private String referencia;
    @Column(length=120,nullable=false) private String registradoPor;
    @Column(nullable=false) private LocalDateTime fecha=LocalDateTime.now();
    public Long getId(){return id;} public Factura getFactura(){return factura;} public void setFactura(Factura v){factura=v;} public BigDecimal getValor(){return valor;} public void setValor(BigDecimal v){valor=v;}
    public String getMedio(){return medio;} public void setMedio(String v){medio=v;} public String getReferencia(){return referencia;} public void setReferencia(String v){referencia=v;}
    public String getRegistradoPor(){return registradoPor;} public void setRegistradoPor(String v){registradoPor=v;} public LocalDateTime getFecha(){return fecha;}
}
