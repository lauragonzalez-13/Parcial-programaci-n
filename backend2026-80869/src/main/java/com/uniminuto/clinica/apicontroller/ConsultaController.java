
package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.ConsultaRequest;
import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.service.ConsultaService;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    // Listar consultas: más recientes primero
    @GetMapping
    public ResponseEntity<List<Consulta>> listar() {
        return ResponseEntity.ok(consultaService.listar());
    }

    @GetMapping("/veterinario/{id}")
    public ResponseEntity<List<Consulta>> porVeterinario(@PathVariable Long id, @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
        return ResponseEntity.ok(consultaService.listarPorVeterinario(id, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay().minusNanos(1)));
    }

    @GetMapping("/mascota/{id}/historial")
    public ResponseEntity<List<Consulta>> historialMascota(@PathVariable Long id) { return ResponseEntity.ok(consultaService.historialMascota(id)); }

    @GetMapping("/reportes/veterinario/{id}")
    public ResponseEntity<byte[]> reporte(@PathVariable Long id, @RequestParam LocalDate desde, @RequestParam LocalDate hasta) throws Exception {
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
        var citas = consultaService.listarPorVeterinario(id, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay().minusNanos(1));
        StringBuilder rows = new StringBuilder("<row r=\"1\">");
        String[] heads={"ID","Fecha y hora inicio","Fecha y hora fin","Propietario","Mascota","Veterinario","Motivo","Estado"};
        for(int i=0;i<heads.length;i++) rows.append(cell(i+1,1,heads[i])); rows.append("</row>");
        int n=2;
        for(var c:citas) {
            String propietario=c.getMascota().getCliente().getNombre()+" "+c.getMascota().getCliente().getApellido();
            String vet=c.getVeterinario().getNombre()+" "+c.getVeterinario().getApellido();
            String[] vals={String.valueOf(c.getId()),c.getFechaHora().toString().replace('T',' '),c.getFechaHora().plusMinutes(c.getDuracionMinutos()).toString().replace('T',' '),propietario,c.getMascota().getNombre(),vet,c.getMotivo(),c.getEstado().name()};
            rows.append("<row r=\"").append(n).append("\">"); for(int i=0;i<vals.length;i++) rows.append(cell(i+1,n,vals[i])); rows.append("</row>"); n++;
        }
        String sheet="<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+rows+"</sheetData></worksheet>";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(bytes)) {
            entry(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Citas\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            entry(zip,"xl/worksheets/sheet1.xml",sheet);
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=citas-veterinario-"+id+".xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes.toByteArray());
    }

    private String cell(int column,int row,String value) { StringBuilder c=new StringBuilder(); int x=column; String col=""; while(x>0){int rem=(x-1)%26; col=(char)('A'+rem)+col; x=(x-1)/26;} return "<c r=\""+col+row+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+xml(value)+"</t></is></c>"; }
    private String xml(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;"); }
    private void entry(ZipOutputStream z,String name,String text) throws Exception { z.putNextEntry(new ZipEntry(name)); z.write(text.getBytes(StandardCharsets.UTF_8)); z.closeEntry(); }

    // Buscar una consulta por ID
    @GetMapping("/{id}")
    public ResponseEntity<Consulta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id));
    }

    // Crear una consulta
    @PostMapping
    public ResponseEntity<Consulta> crear(
            @RequestBody ConsultaRequest request) {

        Consulta consulta = consultaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(consulta);
    }

    // Actualizar una consulta
    @PutMapping("/{id}")
    public ResponseEntity<Consulta> actualizar(
            @PathVariable Long id,
            @RequestBody ConsultaRequest request) {
        return ResponseEntity.ok(
                consultaService.actualizar(id, request));
    }

    // Cancelar una consulta explícitamente
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Consulta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.cancelar(id));
    }

    // Registrar que la cita ocurrió; su información clínica queda asociada a la consulta.
    @PatchMapping("/{id}/atender")
    public ResponseEntity<Consulta> atender(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.atender(id));
    }

    // Errores de validación de datos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", ex.getMessage()));
    }

    // Conflictos de estado o de horario
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> manejarConflictos(
            IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("mensaje", ex.getMessage()));
    }
}
