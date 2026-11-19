package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="linea_factura")
public class LineaFactura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="factura_id",nullable=false) private Factura factura;
    @Column(nullable=false,length=20) private String tipo;
    @Column(nullable=false,length=240) private String descripcion;
    @Column(nullable=false,precision=12,scale=3) private BigDecimal cantidad;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal valorUnitario;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal ivaPorcentaje;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal impuesto;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal total;
    @Column(length=100) private String codigoLote;
    public Long getId(){return id;} public Factura getFactura(){return factura;} public void setFactura(Factura v){factura=v;} public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public BigDecimal getCantidad(){return cantidad;} public void setCantidad(BigDecimal v){cantidad=v;}
    public BigDecimal getValorUnitario(){return valorUnitario;} public void setValorUnitario(BigDecimal v){valorUnitario=v;} public BigDecimal getIvaPorcentaje(){return ivaPorcentaje;} public void setIvaPorcentaje(BigDecimal v){ivaPorcentaje=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;} public BigDecimal getImpuesto(){return impuesto;} public void setImpuesto(BigDecimal v){impuesto=v;} public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
    public String getCodigoLote(){return codigoLote;} public void setCodigoLote(String v){codigoLote=v;}
}
