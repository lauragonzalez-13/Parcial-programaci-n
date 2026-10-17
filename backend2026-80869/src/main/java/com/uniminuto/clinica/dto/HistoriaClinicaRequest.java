package com.uniminuto.clinica.dto;

public record HistoriaClinicaRequest(
        Long clienteId,
        Long mascotaId,
        Long consultaId,
        String descripcion,
        String observacion,
        String diagnostico) { }
