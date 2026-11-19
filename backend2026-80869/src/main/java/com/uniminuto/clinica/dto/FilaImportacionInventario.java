package com.uniminuto.clinica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FilaImportacionInventario(int fila, String codigo, String nombre, String descripcion,
        String presentacion, String tipo, String registroInvima, BigDecimal stockMinimo,
        LocalDate fechaProximoPedido, String codigoLote, BigDecimal cantidad, BigDecimal precioCompra,
        BigDecimal precioVenta, LocalDate fechaIngreso, LocalDate fechaVencimiento,
        List<String> errores) { }
