package com.uniminuto.clinica.dto;

public record AnotacionClinicaRequest(
        Long consultaId,
        String descripcion,
        String observacion,
        String diagnostico) { }
