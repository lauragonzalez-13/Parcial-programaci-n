package com.uniminuto.clinica.dto;

public record ValidarTokenResponse(Long usuarioId, String token, String username, boolean logueoExitoso, String rol, Long veterinarioId) {}
