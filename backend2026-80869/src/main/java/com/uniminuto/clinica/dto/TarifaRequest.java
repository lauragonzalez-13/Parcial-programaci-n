package com.uniminuto.clinica.dto;
import java.math.BigDecimal;
public record TarifaRequest(String codigo, String nombre, String descripcion, String tipoServicio, Long especialidadId, BigDecimal valor, BigDecimal ivaPorcentaje, boolean activa, String usuario) { }
