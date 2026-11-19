package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "importacion_inventario")
@Data
public class ImportacionInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 120)
    private String usuarioResponsable;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false)
    private Integer registrosLeidos = 0;

    @Column(nullable = false)
    private Integer registrosImportados = 0;

    @Column(nullable = false)
    private Integer registrosRechazados = 0;

    @Column(columnDefinition = "text")
    private String detalleErrores;
}
