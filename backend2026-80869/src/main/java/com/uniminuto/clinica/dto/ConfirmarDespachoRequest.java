package com.uniminuto.clinica.dto;

import java.util.List;

public record ConfirmarDespachoRequest(String usuarioResponsable, String observacion, List<DetalleEntregaDespachoRequest> detalles) { }
