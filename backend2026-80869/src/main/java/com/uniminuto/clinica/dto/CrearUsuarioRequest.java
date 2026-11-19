package com.uniminuto.clinica.dto;

public record CrearUsuarioRequest(String username, String password, String rol, Long veterinarioId) {}
