package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "formula_medica")
@Data
public class FormulaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false, length = 150)
    private String medicamento;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false, length = 100)
    private String frecuencia;

    @Column(nullable = false)
    private Integer duracionDias;

    @Column(nullable = false, length = 1000)
    private String indicaciones;

    @Column(length = 1000)
    private String observaciones;

    @Column(name = "via_administracion", length = 80)
    private String viaAdministracion;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne
    @JoinColumn(name = "medicamento_inventario_id")
    private Medicamento medicamentoInventario;

    @Column(precision = 12, scale = 3)
    private BigDecimal cantidadRequerida;

    @Column(precision = 12, scale = 3)
    private BigDecimal unidadesPorToma;

    private Integer tomasPorDia;

    @Column(precision = 12, scale = 3)
    private BigDecimal cantidadDispensada = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoDispensacion estadoDispensacion = EstadoDispensacion.PENDIENTE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;
}
