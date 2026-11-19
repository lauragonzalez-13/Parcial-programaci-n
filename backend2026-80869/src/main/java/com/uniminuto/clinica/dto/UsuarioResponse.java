package com.uniminuto.clinica.dto;

public record UsuarioResponse(Long usuarioId, String username, String rol, Long veterinarioId, boolean activo) {}
