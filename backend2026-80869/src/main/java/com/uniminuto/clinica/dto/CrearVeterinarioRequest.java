package com.uniminuto.clinica.dto;

public record CrearVeterinarioRequest(String nombre, String apellido, String documento,
        String tarjetaProfesional, String correo) {}
