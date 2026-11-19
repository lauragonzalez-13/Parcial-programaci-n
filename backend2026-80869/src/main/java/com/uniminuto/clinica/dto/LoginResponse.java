package com.uniminuto.clinica.dto;

public record LoginResponse(String username, Long usuarioId, boolean login, String token, String rol, Long veterinarioId) {}
