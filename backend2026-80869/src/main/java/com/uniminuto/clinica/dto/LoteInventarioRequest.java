package com.uniminuto.clinica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteInventarioRequest(Long medicamentoId, String codigoLote, BigDecimal cantidad,
        BigDecimal precioCompra, BigDecimal precioVenta, LocalDate fechaIngreso,
        LocalDate fechaVencimiento, String usuarioResponsable, String observacion) { }
