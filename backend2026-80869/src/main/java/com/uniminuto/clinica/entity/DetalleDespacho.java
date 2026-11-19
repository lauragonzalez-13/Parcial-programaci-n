package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="detalle_despacho")
public class DetalleDespacho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="despacho_id", nullable=false) private Despacho despacho;
    @ManyToOne(optional=false) @JoinColumn(name="lote_id", nullable=false) private LoteInventario lote;
    @OneToOne(optional=false) @JoinColumn(name="movimiento_id", nullable=false, unique=true) private MovimientoInventario movimiento;
    @Column(nullable=false, precision=12, scale=3) private BigDecimal cantidad;
    @Column(nullable=false) private LocalDateTime fecha=LocalDateTime.now();
    public Long getId(){return id;} public Despacho getDespacho(){return despacho;} public void setDespacho(Despacho v){despacho=v;}
    public LoteInventario getLote(){return lote;} public void setLote(LoteInventario v){lote=v;}
    public MovimientoInventario getMovimiento(){return movimiento;} public void setMovimiento(MovimientoInventario v){movimiento=v;}
    public BigDecimal getCantidad(){return cantidad;} public void setCantidad(BigDecimal v){cantidad=v;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime v){fecha=v;}
}
