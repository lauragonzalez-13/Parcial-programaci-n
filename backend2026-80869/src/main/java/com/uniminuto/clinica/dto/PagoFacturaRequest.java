package com.uniminuto.clinica.dto;
import java.math.BigDecimal;
public record PagoFacturaRequest(BigDecimal valor, String medio, String referencia, String registradoPor) { }
