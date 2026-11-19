package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "lote_inventario", uniqueConstraints = @UniqueConstraint(
        name = "uk_lote_producto_codigo", columnNames = {"medicamento_id", "codigo_lote"}))
@Data
public class LoteInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(name = "codigo_lote", nullable = false, length = 80)
    private String codigoLote;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadDisponible;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioCompra;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    private LocalDate fechaVencimiento;
}
