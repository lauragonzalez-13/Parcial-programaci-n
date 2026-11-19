package com.uniminuto.clinica.dto;

import java.util.List;

public record PrevisualizacionImportacion(String token, int registrosValidos, int registrosConError,
        List<FilaImportacionInventario> filas) { }
