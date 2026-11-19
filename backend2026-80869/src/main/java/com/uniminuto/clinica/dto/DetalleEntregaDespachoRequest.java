package com.uniminuto.clinica.dto;

import java.math.BigDecimal;

public record DetalleEntregaDespachoRequest(Long loteId, BigDecimal cantidad) { }
