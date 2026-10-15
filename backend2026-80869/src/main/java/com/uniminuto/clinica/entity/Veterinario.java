package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "veterinario")
@Data
public class Veterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(length = 30)
    private String tarjetaProfesional;

    @Column(length = 100)
    private String correo;

    @Column(nullable = false)
    private boolean activo = true;
}