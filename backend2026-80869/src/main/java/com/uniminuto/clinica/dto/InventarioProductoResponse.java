package com.uniminuto.clinica.dto;

import com.uniminuto.clinica.entity.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventarioProductoResponse(Long id, String codigo, String nombre, String descripcion,
        String presentacion, TipoProducto tipo, String registroInvima, BigDecimal stockMinimo,
        LocalDate fechaProximoPedido, BigDecimal cantidadDisponible, BigDecimal precioCompraMinimo,
        BigDecimal precioVenta, LocalDate proximoVencimiento, String estado, boolean activo, BigDecimal ivaPorcentaje) { }
