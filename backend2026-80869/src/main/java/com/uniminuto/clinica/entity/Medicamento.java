package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "medicamento")
@Data
public class Medicamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 8)
    private String codigo;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(length = 100)
    private String presentacion;

    // Columnas heredadas de la tabla medicamento; las fechas vigentes pertenecen a cada lote.
    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "fecha_vence")
    private LocalDate fechaVence;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoProducto tipo = TipoProducto.MEDICAMENTO;

    @Column(length = 80)
    private String registroInvima;

    @Column(precision = 12, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "iva_porcentaje", precision = 5, scale = 2, nullable = false)
    private BigDecimal ivaPorcentaje = BigDecimal.ZERO;

    private LocalDate fechaProximoPedido;

    @Column
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacionRegistro = LocalDateTime.now();

    private LocalDateTime fechaModificacionRegistro;

    @PrePersist
    void crearFechaRegistro() {
        if (fechaCreacionRegistro == null) fechaCreacionRegistro = LocalDateTime.now();
        validarInvima();
    }

    @PreUpdate
    void modificarFechaRegistro() {
        fechaModificacionRegistro = LocalDateTime.now();
        validarInvima();
    }

    private void validarInvima() {
        if (tipo == TipoProducto.MEDICAMENTO && (registroInvima == null || registroInvima.isBlank())) {
            throw new IllegalStateException("El registro Invima es obligatorio para los medicamentos.");
        }
    }
}
