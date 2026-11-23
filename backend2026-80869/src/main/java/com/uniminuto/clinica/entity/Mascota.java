package com.uniminuto.clinica.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "mascota")
@Data
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String especie;

    @Column(length = 50)
    private String raza;

    @Column(length = 20)
    private String sexo;

    private Integer edad;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;
}
