package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "movimiento_inventario")
@Data
public class MovimientoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteInventario lote;

    @ManyToOne
    @JoinColumn(name = "formula_medica_id")
    private FormulaMedica formulaMedica;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "despacho_id")
    private Despacho despacho;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, length = 120)
    private String usuarioResponsable;

    @Column(length = 300)
    private String observacion;
}
