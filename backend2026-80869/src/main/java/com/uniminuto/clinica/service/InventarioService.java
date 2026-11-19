package com.uniminuto.clinica.service;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class InventarioService {
    private static final Map<String, ImportacionPendiente> IMPORTACIONES = new ConcurrentHashMap<>();
    private static final List<String> ENCABEZADOS = List.of("codigo", "nombre", "tipo", "registroinvima",
            "stockminimo", "fechaproximopedido", "descripcion", "presentacion", "codigolote", "cantidad",
            "preciocompra", "precioventa", "fechaingreso", "fechavencimiento");

    private final MedicamentoRepository medicamentos;
    private final LoteInventarioRepository lotes;
    private final MovimientoInventarioRepository movimientos;
    private final ImportacionInventarioRepository importaciones;
    private final TransactionTemplate transaccion;

    public InventarioService(MedicamentoRepository medicamentos, LoteInventarioRepository lotes,
            MovimientoInventarioRepository movimientos, ImportacionInventarioRepository importaciones,
            PlatformTransactionManager txManager) {
        this.medicamentos = medicamentos;
        this.lotes = lotes;
        this.movimientos = movimientos;
        this.importaciones = importaciones;
        this.transaccion = new TransactionTemplate(txManager);
    }

    @Transactional(readOnly = true)
    public List<InventarioProductoResponse> listar(boolean disponibles) {
        return medicamentos.findAll().stream().filter(Medicamento::isActivo)
                .map(this::resumen).filter(x -> !disponibles || x.cantidadDisponible().signum() > 0).toList();
    }

    @Transactional
    public Medicamento guardar(MedicamentoRequest request, Long id) {
        validarProducto(request);
        Medicamento entidad = id == null ? new Medicamento() : medicamentos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
        validarCodigoNombre(request, id);
        entidad.setCodigo(request.codigo().trim().toUpperCase(Locale.ROOT));
        entidad.setNombre(request.nombre().trim());
        entidad.setDescripcion(limpiar(request.descripcion()));
        entidad.setPresentacion(limpiar(request.presentacion()));
        entidad.setTipo(request.tipo());
        entidad.setRegistroInvima(limpiar(request.registroInvima()));
        entidad.setStockMinimo(request.stockMinimo());
        if (request.ivaPorcentaje() != null) entidad.setIvaPorcentaje(request.ivaPorcentaje());
        entidad.setFechaProximoPedido(request.fechaProximoPedido());
        return medicamentos.save(entidad);
    }

    @Transactional
    public Medicamento actualizarImpuesto(Long medicamentoId, BigDecimal ivaPorcentaje) {
        if (ivaPorcentaje == null || ivaPorcentaje.signum() < 0 || ivaPorcentaje.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("El IVA del producto debe estar entre 0 y 100 por ciento.");
        Medicamento producto=medicamentos.findById(medicamentoId).orElseThrow(()->new IllegalArgumentException("El producto no existe."));
        producto.setIvaPorcentaje(ivaPorcentaje);return medicamentos.save(producto);
    }

    @Transactional
    public LoteInventario ingresar(LoteInventarioRequest request) {
        Medicamento producto = medicamentos.findById(request.medicamentoId())
                .orElseThrow(() -> new IllegalArgumentException("El medicamento o producto no existe."));
        validarLote(request);
        if (lotes.existsByMedicamentoIdAndCodigoLoteIgnoreCase(producto.getId(), request.codigoLote().trim())) {
            throw new IllegalArgumentException("Ya existe ese código de lote para el producto.");
        }
        LoteInventario lote = new LoteInventario();
        lote.setMedicamento(producto);
        lote.setCodigoLote(request.codigoLote().trim());
        lote.setCantidadDisponible(request.cantidad());
        lote.setPrecioCompra(request.precioCompra());
        lote.setPrecioVenta(request.precioVenta());
        lote.setFechaIngreso(request.fechaIngreso());
        lote.setFechaVencimiento(request.fechaVencimiento());
        lote = lotes.save(lote);
        registrarMovimiento(producto, lote, TipoMovimiento.ENTRADA, request.cantidad(), null,
                usuario(request.usuarioResponsable()), request.observacion());
        return lote;
    }

    @Transactional(readOnly = true)
    public List<LoteInventario> listarLotes(Long medicamentoId) {
        return medicamentoId == null ? lotes.findAll() : lotes.findByMedicamentoIdOrderByFechaVencimientoAsc(medicamentoId);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarMovimientos(Long medicamentoId) {
        return medicamentoId == null ? movimientos.findAllByOrderByFechaDesc()
                : movimientos.findByMedicamentoIdOrderByFechaDesc(medicamentoId);
    }

    @Transactional(readOnly = true)
    public List<InventarioProductoResponse> alertas() {
        return listar(false).stream().filter(p -> !"DISPONIBLE".equals(p.estado())).toList();
    }

    public PrevisualizacionImportacion previsualizar(MultipartFile archivo, String usuario) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Selecciona un archivo CSV o XLSX.");
        String nombreArchivo = Objects.requireNonNullElse(archivo.getOriginalFilename(), "inventario");
        if (!(nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".csv") || nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".xlsx"))) {
            throw new IllegalArgumentException("El archivo debe tener extensión .csv o .xlsx.");
        }
        if (archivo.getSize() > 15 * 1024 * 1024) throw new IllegalArgumentException("El archivo no puede superar 15 MB.");
        try {
            List<List<String>> tabla = nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".csv")
                    ? leerCsv(archivo.getBytes()) : leerXlsx(archivo.getBytes());
            if (tabla.size() < 2) throw new IllegalArgumentException("El archivo debe incluir encabezados y al menos un registro.");
            Map<String, Integer> columnas = validarEncabezados(tabla.get(0));
            List<FilaImportacionInventario> filas = new ArrayList<>();
            Set<String> lotesArchivo = new HashSet<>();
            for (int i = 1; i < tabla.size(); i++) {
                List<String> celdas = tabla.get(i);
                if (celdas.stream().allMatch(String::isBlank)) continue;
                FilaImportacionInventario fila = mapearFila(i + 1, celdas, columnas);
                List<String> errores = new ArrayList<>(fila.errores());
                validarFila(fila, errores);
                String loteKey = Objects.toString(fila.codigo(), "").toUpperCase(Locale.ROOT) + "|" + Objects.toString(fila.codigoLote(), "").toUpperCase(Locale.ROOT);
                Long productoExistenteId = fila.codigo() == null ? null : medicamentos.findByCodigoIgnoreCase(fila.codigo()).map(Medicamento::getId).orElse(null);
                if (!loteKey.endsWith("|") && (!lotesArchivo.add(loteKey) || (productoExistenteId != null && lotes.existsByMedicamentoIdAndCodigoLoteIgnoreCase(productoExistenteId, fila.codigoLote())))) {
                    errores.add("El código de lote ya está repetido para este producto.");
                }
                validarIdentidadExistente(fila, errores);
                filas.add(conErrores(fila, errores));
            }
            if (filas.isEmpty()) throw new IllegalArgumentException("El archivo no contiene filas de datos.");
            String token = UUID.randomUUID().toString();
            IMPORTACIONES.put(token, new ImportacionPendiente(nombreArchivo, usuario(usuario), filas));
            int validas = (int) filas.stream().filter(f -> f.errores().isEmpty()).count();
            return new PrevisualizacionImportacion(token, validas, filas.size() - validas, filas);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el archivo. Verifica que sea CSV UTF-8 o XLSX válido.", e);
        }
    }

    public ImportacionInventario confirmar(String token, String responsable) {
        ImportacionPendiente pendiente = IMPORTACIONES.remove(token);
        if (pendiente == null) throw new IllegalArgumentException("La vista previa expiró o ya fue confirmada. Carga el archivo de nuevo.");
        String usuario = usuario(responsable == null ? pendiente.usuario() : responsable);
        List<String> errores = new ArrayList<>();
        int importados = 0;
        int rechazados = 0;
        for (FilaImportacionInventario fila : pendiente.filas()) {
            List<String> filaErrores = new ArrayList<>(fila.errores());
            try {
                if (filaErrores.isEmpty()) {
                    Boolean ok = transaccion.execute(status -> {
                        Medicamento producto = buscarOCrearProducto(fila);
                        if (lotes.existsByMedicamentoIdAndCodigoLoteIgnoreCase(producto.getId(), fila.codigoLote())) {
                            throw new IllegalArgumentException("El lote ya fue registrado por otra importación.");
                        }
                        LoteInventarioRequest request = new LoteInventarioRequest(producto.getId(), fila.codigoLote(),
                                fila.cantidad(), fila.precioCompra(), fila.precioVenta(), fila.fechaIngreso(),
                                fila.fechaVencimiento(), usuario, "Importación " + pendiente.nombreArchivo());
                        ingresar(request);
                        return true;
                    });
                    if (Boolean.TRUE.equals(ok)) importados++;
                } else rechazados++;
            } catch (RuntimeException e) {
                rechazados++;
                filaErrores.add(Objects.requireNonNullElse(e.getMessage(), "Error al guardar el registro."));
            }
            if (!filaErrores.isEmpty()) errores.add("Fila " + fila.fila() + ": " + String.join("; ", filaErrores));
        }
        ImportacionInventario log = new ImportacionInventario();
        log.setNombreArchivo(pendiente.nombreArchivo());
        log.setUsuarioResponsable(usuario);
        log.setFecha(LocalDateTime.now());
        log.setRegistrosLeidos(pendiente.filas().size());
        log.setRegistrosImportados(importados);
        log.setRegistrosRechazados(rechazados);
        log.setDetalleErrores(String.join("\n", errores));
        return importaciones.save(log);
    }

    @Transactional(readOnly = true)
    public List<ImportacionInventario> listarImportaciones() { return importaciones.findAllByOrderByFechaDesc(); }

    private InventarioProductoResponse resumen(Medicamento p) {
        List<LoteInventario> delProducto = lotes.findByMedicamentoIdOrderByFechaVencimientoAsc(p.getId());
        List<LoteInventario> utilizables = delProducto.stream().filter(l -> l.getFechaVencimiento() == null
                || !l.getFechaVencimiento().isBefore(LocalDate.now())).toList();
        BigDecimal cantidad = utilizables.stream().map(LoteInventario::getCantidadDisponible).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal compra = delProducto.stream().map(LoteInventario::getPrecioCompra).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        LoteInventario precioVigente = utilizables.stream().filter(l -> l.getCantidadDisponible().signum() > 0)
                .max(Comparator.comparing(LoteInventario::getFechaIngreso)).orElse(null);
        LocalDate vencimiento = utilizables.stream().filter(l -> l.getCantidadDisponible().signum() > 0)
                .map(LoteInventario::getFechaVencimiento).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
        BigDecimal minimo = p.getStockMinimo() == null ? BigDecimal.ZERO : p.getStockMinimo();
        boolean vencido = cantidad.signum() == 0 && delProducto.stream().anyMatch(l -> l.getCantidadDisponible().signum() > 0);
        String estado = vencido ? "VENCIDO" : cantidad.signum() == 0 ? "AGOTADO"
                : cantidad.compareTo(minimo) <= 0 ? "STOCK_BAJO"
                : vencimiento != null && !vencimiento.isAfter(LocalDate.now().plusDays(30)) ? "POR_VENCER" : "DISPONIBLE";
        return new InventarioProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getPresentacion(), p.getTipo(), p.getRegistroInvima(), p.getStockMinimo(), p.getFechaProximoPedido(),
                cantidad, compra, precioVigente == null ? BigDecimal.ZERO : precioVigente.getPrecioVenta(), vencimiento,
                estado, p.isActivo(), p.getIvaPorcentaje());
    }

    private void registrarMovimiento(Medicamento producto, LoteInventario lote, TipoMovimiento tipo,
            BigDecimal cantidad, FormulaMedica formula, String responsable, String observacion) {
        MovimientoInventario m = new MovimientoInventario();
        m.setMedicamento(producto); m.setLote(lote); m.setTipo(tipo); m.setCantidad(cantidad);
        m.setFormulaMedica(formula); m.setFecha(LocalDateTime.now()); m.setUsuarioResponsable(usuario(responsable));
        m.setObservacion(limpiar(observacion));
        movimientos.save(m);
    }

    private void validarProducto(MedicamentoRequest r) {
        if (r == null || r.codigo() == null || !r.codigo().trim().matches("(?i)[a-z0-9]{8}")) throw new IllegalArgumentException("El código debe tener exactamente 8 caracteres alfanuméricos.");
        if (r.nombre() == null || r.nombre().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio.");
        if (r.tipo() == null) throw new IllegalArgumentException("Selecciona si es medicamento o insumo.");
        if (r.tipo() == TipoProducto.MEDICAMENTO && (r.registroInvima() == null || r.registroInvima().isBlank())) throw new IllegalArgumentException("El registro Invima es obligatorio para medicamentos.");
        if (r.stockMinimo() == null || r.stockMinimo().signum() < 0) throw new IllegalArgumentException("El stock mínimo debe ser cero o mayor.");
    }

    private void validarCodigoNombre(MedicamentoRequest r, Long omitido) {
        medicamentos.findByCodigoIgnoreCase(r.codigo().trim()).filter(m -> !Objects.equals(m.getId(), omitido))
                .ifPresent(m -> { throw new IllegalArgumentException("Ya existe un producto con ese código."); });
        medicamentos.findByNombreIgnoreCase(r.nombre().trim()).filter(m -> !Objects.equals(m.getId(), omitido))
                .ifPresent(m -> { throw new IllegalArgumentException("Ya existe un producto con ese nombre."); });
    }

    private void validarLote(LoteInventarioRequest r) {
        if (r == null || r.codigoLote() == null || r.codigoLote().isBlank()) throw new IllegalArgumentException("El código del lote es obligatorio.");
        if (r.cantidad() == null || r.cantidad().signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        if (r.precioCompra() == null || r.precioCompra().signum() < 0 || r.precioVenta() == null || r.precioVenta().signum() < 0) throw new IllegalArgumentException("Los precios deben ser cero o mayores.");
        if (r.fechaIngreso() == null) throw new IllegalArgumentException("La fecha de ingreso es obligatoria.");
        if (r.fechaVencimiento() != null && r.fechaVencimiento().isBefore(r.fechaIngreso())) throw new IllegalArgumentException("El vencimiento no puede ser anterior al ingreso.");
    }

    private Map<String, Integer> validarEncabezados(List<String> encabezados) {
        Map<String, Integer> mapa = new HashMap<>();
        for (int i = 0; i < encabezados.size(); i++) mapa.put(normalizar(encabezados.get(i)), i);
        List<String> faltantes = ENCABEZADOS.stream().filter(h -> !mapa.containsKey(h)).toList();
        if (!faltantes.isEmpty()) throw new IllegalArgumentException("Faltan encabezados obligatorios: " + String.join(", ", faltantes));
        return mapa;
    }

    private FilaImportacionInventario mapearFila(int numero, List<String> c, Map<String, Integer> ix) {
        List<String> errores = new ArrayList<>();
        try {
            return new FilaImportacionInventario(numero, celda(c, ix, "codigo"), celda(c, ix, "nombre"),
                    celda(c, ix, "descripcion"), celda(c, ix, "presentacion"), celda(c, ix, "tipo"),
                    celda(c, ix, "registroinvima"), decimal(celda(c, ix, "stockminimo")),
                    fecha(celda(c, ix, "fechaproximopedido")), celda(c, ix, "codigolote"),
                    decimal(celda(c, ix, "cantidad")), decimal(celda(c, ix, "preciocompra")),
                    decimal(celda(c, ix, "precioventa")), fecha(celda(c, ix, "fechaingreso")),
                    fecha(celda(c, ix, "fechavencimiento")), errores);
        } catch (RuntimeException e) {
            errores.add(Objects.requireNonNullElse(e.getMessage(), "Formato numérico o de fecha inválido."));
            return new FilaImportacionInventario(numero, celda(c, ix, "codigo"), celda(c, ix, "nombre"),
                    celda(c, ix, "descripcion"), celda(c, ix, "presentacion"), celda(c, ix, "tipo"),
                    celda(c, ix, "registroinvima"), null, null, celda(c, ix, "codigolote"), null, null, null,
                    null, null, errores);
        }
    }

    private void validarFila(FilaImportacionInventario f, List<String> e) {
        if (f.codigo() == null || !f.codigo().matches("(?i)[a-z0-9]{8}")) e.add("El código debe tener 8 caracteres alfanuméricos.");
        if (f.nombre() == null || f.nombre().isBlank()) e.add("El nombre es obligatorio.");
        if (f.tipo() == null || !(f.tipo().equalsIgnoreCase("MEDICAMENTO") || f.tipo().equalsIgnoreCase("INSUMO"))) e.add("El tipo debe ser MEDICAMENTO o INSUMO.");
        if (f.tipo() != null && f.tipo().equalsIgnoreCase("MEDICAMENTO") && (f.registroInvima() == null || f.registroInvima().isBlank())) e.add("El registro Invima es obligatorio para medicamentos.");
        if (f.codigoLote() == null || f.codigoLote().isBlank()) e.add("El código de lote es obligatorio.");
        if (f.cantidad() == null || f.cantidad().signum() <= 0) e.add("La cantidad debe ser mayor que cero.");
        if (f.stockMinimo() == null || f.stockMinimo().signum() < 0) e.add("El stock mínimo debe ser cero o mayor.");
        if (f.precioCompra() == null || f.precioCompra().signum() < 0 || f.precioVenta() == null || f.precioVenta().signum() < 0) e.add("Los precios deben ser cero o mayores.");
        if (f.fechaIngreso() == null) e.add("La fecha de ingreso es obligatoria.");
        if (f.fechaVencimiento() != null && f.fechaIngreso() != null && f.fechaVencimiento().isBefore(f.fechaIngreso())) e.add("El vencimiento no puede ser anterior al ingreso.");
    }

    private void validarIdentidadExistente(FilaImportacionInventario f, List<String> e) {
        Optional<Medicamento> porCodigo = medicamentos.findByCodigoIgnoreCase(f.codigo());
        Optional<Medicamento> porNombre = medicamentos.findByNombreIgnoreCase(f.nombre());
        if (porCodigo.isPresent() && porNombre.isPresent() && !porCodigo.get().getId().equals(porNombre.get().getId())) e.add("El código y el nombre pertenecen a productos distintos.");
        else if (porCodigo.isPresent() && !porCodigo.get().getNombre().equalsIgnoreCase(f.nombre())) e.add("El código ya está asociado a otro nombre.");
        else if (porNombre.isPresent() && !porNombre.get().getCodigo().equalsIgnoreCase(f.codigo())) e.add("El nombre ya está asociado a otro código.");
    }

    private Medicamento buscarOCrearProducto(FilaImportacionInventario f) {
        Optional<Medicamento> existente = medicamentos.findByCodigoIgnoreCase(f.codigo());
        if (existente.isPresent()) return existente.get();
        Medicamento p = new Medicamento(); p.setCodigo(f.codigo().toUpperCase(Locale.ROOT)); p.setNombre(f.nombre().trim());
        p.setDescripcion(limpiar(f.descripcion())); p.setPresentacion(limpiar(f.presentacion()));
        p.setTipo(TipoProducto.valueOf(f.tipo().toUpperCase(Locale.ROOT))); p.setRegistroInvima(limpiar(f.registroInvima()));
        p.setStockMinimo(f.stockMinimo()); p.setFechaProximoPedido(f.fechaProximoPedido());
        return medicamentos.save(p);
    }

    private FilaImportacionInventario conErrores(FilaImportacionInventario f, List<String> e) {
        return new FilaImportacionInventario(f.fila(), f.codigo(), f.nombre(), f.descripcion(), f.presentacion(), f.tipo(), f.registroInvima(), f.stockMinimo(), f.fechaProximoPedido(), f.codigoLote(), f.cantidad(), f.precioCompra(), f.precioVenta(), f.fechaIngreso(), f.fechaVencimiento(), List.copyOf(e));
    }

    private List<List<String>> leerCsv(byte[] bytes) {
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).replace("\uFEFF", "");
        String primeraLinea = text.lines().findFirst().orElse("");
        char separador = primeraLinea.chars().filter(c -> c == ';').count() > primeraLinea.chars().filter(c -> c == ',').count() ? ';' : ',';
        List<List<String>> filas = new ArrayList<>(); List<String> fila = new ArrayList<>(); StringBuilder celda = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' && quote && i + 1 < text.length() && text.charAt(i + 1) == '"') { celda.append('"'); i++; }
            else if (ch == '"') quote = !quote;
            else if (ch == separador && !quote) { fila.add(celda.toString().trim()); celda.setLength(0); }
            else if ((ch == '\n' || ch == '\r') && !quote) {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                fila.add(celda.toString().trim()); celda.setLength(0); filas.add(fila); fila = new ArrayList<>();
            } else celda.append(ch);
        }
        if (!fila.isEmpty() || !celda.isEmpty()) { fila.add(celda.toString().trim()); filas.add(fila); }
        return filas;
    }

    private List<List<String>> leerXlsx(byte[] bytes) throws Exception {
        Map<String, byte[]> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("xl/sharedStrings.xml") || entry.getName().equals("xl/worksheets/sheet1.xml")) partes.put(entry.getName(), zip.readAllBytes());
            }
        }
        if (!partes.containsKey("xl/worksheets/sheet1.xml")) throw new IllegalArgumentException("El XLSX no contiene la primera hoja de datos.");
        List<String> shared = new ArrayList<>();
        if (partes.containsKey("xl/sharedStrings.xml")) {
            NodeList sis = xml(partes.get("xl/sharedStrings.xml")).getElementsByTagName("si");
            for (int i = 0; i < sis.getLength(); i++) shared.add(textoDescendiente((Element) sis.item(i), "t"));
        }
        NodeList rows = xml(partes.get("xl/worksheets/sheet1.xml")).getElementsByTagName("row");
        List<List<String>> tabla = new ArrayList<>();
        for (int r = 0; r < rows.getLength(); r++) {
            Element row = (Element) rows.item(r); NodeList cells = row.getElementsByTagName("c"); Map<Integer, String> valores = new HashMap<>(); int max = -1;
            for (int c = 0; c < cells.getLength(); c++) {
                Element cell = (Element) cells.item(c); int column = columna(cell.getAttribute("r")); max = Math.max(max, column);
                String value = textoDescendiente(cell, "v");
                if ("s".equals(cell.getAttribute("t")) && !value.isBlank()) value = shared.get(Integer.parseInt(value));
                else if ("inlineStr".equals(cell.getAttribute("t"))) value = textoDescendiente(cell, "t");
                valores.put(column, value);
            }
            List<String> values = new ArrayList<>(); for (int c = 0; c <= max; c++) values.add(valores.getOrDefault(c, "")); tabla.add(values);
        }
        return tabla;
    }

    private org.w3c.dom.Document xml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private String textoDescendiente(Element e, String tag) { NodeList nodes = e.getElementsByTagName(tag); return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent(); }
    private int columna(String referencia) { int value = 0; for (char c : referencia.replaceAll("\\d", "").toCharArray()) value = value * 26 + (Character.toUpperCase(c) - 'A' + 1); return value - 1; }
    private String celda(List<String> c, Map<String, Integer> ix, String h) { int i = ix.get(h); return i < c.size() ? c.get(i).trim() : ""; }
    private String normalizar(String s) { return s == null ? "" : java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT); }
    private BigDecimal decimal(String s) { if (s == null || s.isBlank()) return null; return new BigDecimal(s.trim().replace(',', '.')); }
    private LocalDate fecha(String s) {
        if (s == null || s.isBlank()) return null;
        String valor = s.trim();
        try { return LocalDate.parse(valor); }
        catch (java.time.format.DateTimeParseException ex) {
            try { return LocalDate.of(1899, 12, 30).plusDays(Long.parseLong(valor)); }
            catch (NumberFormatException ignored) { throw ex; }
        }
    }
    private String limpiar(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private String usuario(String s) { return s == null || s.isBlank() ? "usuario-no-autenticado" : s.trim(); }
    private record ImportacionPendiente(String nombreArchivo, String usuario, List<FilaImportacionInventario> filas) { }
}
