package com.uniminuto.clinica.dto;

import com.uniminuto.clinica.entity.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicamentoRequest(String codigo, String nombre, String descripcion, String presentacion,
        TipoProducto tipo, String registroInvima, BigDecimal stockMinimo, LocalDate fechaProximoPedido,
        BigDecimal ivaPorcentaje) { }
