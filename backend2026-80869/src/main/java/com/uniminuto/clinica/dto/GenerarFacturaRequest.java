package com.uniminuto.clinica.dto;
import java.math.BigDecimal;
import java.util.List;
public record GenerarFacturaRequest(BigDecimal descuento,String autorizadoPor,List<Long> tarifasAdicionales) { }
