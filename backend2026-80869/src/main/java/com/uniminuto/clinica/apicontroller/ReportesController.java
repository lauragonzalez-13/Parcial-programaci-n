package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.serviceimpl.ReportesService;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reportes")
public class ReportesController {
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final ReportesService service;
    public ReportesController(ReportesService service){this.service=service;}

    @GetMapping("/{tipo}")
    public Map<String,Object> consultar(@PathVariable String tipo,@RequestParam LocalDate desde,@RequestParam LocalDate hasta,
            @RequestParam(required=false) Long veterinarioId,@RequestParam(required=false) Long especialidadId,@RequestParam(required=false) Long medicamentoId){
        return service.generar(tipo,desde,hasta,veterinarioId,especialidadId,medicamentoId);
    }

    @GetMapping("/{tipo}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String tipo,@RequestParam LocalDate desde,@RequestParam LocalDate hasta,
            @RequestParam(required=false) Long veterinarioId,@RequestParam(required=false) Long especialidadId,@RequestParam(required=false) Long medicamentoId){
        Map<String,Object> r=service.generar(tipo,desde,hasta,veterinarioId,especialidadId,medicamentoId);List<String> lines=new ArrayList<>();
        lines.add("Periodo: "+desde+" a "+hasta);lines.add("Generado: "+r.get("generado"));lines.add("Resumen:");
        ((Map<?,?>)r.get("totales")).forEach((k,v)->lines.add(k+": "+v));lines.add("Detalle:");
        for(Object item:(List<?>)r.get("filas"))lines.add(item.toString());
        byte[] bytes=pdfCompleto(String.valueOf(r.get("tipo")),lines);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=reporte-"+tipo.toLowerCase(Locale.ROOT)+".pdf").body(bytes);
    }

    @GetMapping("/{tipo}/excel")
    public ResponseEntity<byte[]> excel(@PathVariable String tipo,@RequestParam LocalDate desde,@RequestParam LocalDate hasta,
            @RequestParam(required=false) Long veterinarioId,@RequestParam(required=false) Long especialidadId,@RequestParam(required=false) Long medicamentoId)throws Exception{
        Map<String,Object> r=service.generar(tipo,desde,hasta,veterinarioId,especialidadId,medicamentoId);
        byte[] bytes=workbook(r);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(XLSX)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=reporte-"+tipo.toLowerCase(Locale.ROOT)+".xlsx").body(bytes);
    }

    private byte[] workbook(Map<String,Object> report)throws Exception{
        StringBuilder rows=new StringBuilder();int row=1;
        row=addRow(rows,row,List.of(String.valueOf(report.get("tipo"))));
        row=addRow(rows,row,List.of("Fecha inicial",String.valueOf(report.get("desde")),"Fecha final",String.valueOf(report.get("hasta")),"Generado",String.valueOf(report.get("generado"))));
        row=addRow(rows,row,List.of("RESUMEN"));
        for(var e:((Map<?,?>)report.get("totales")).entrySet())row=addRow(rows,row,List.of(String.valueOf(e.getKey()),String.valueOf(e.getValue())));
        row=addRow(rows,row,List.of("DETALLE"));
        List<?> data=(List<?>)report.get("filas");
        LinkedHashSet<String> keys=new LinkedHashSet<>();
        for(Object item:data) {
            Map<?,?> itemMap=(Map<?,?>)item;
            for(Object key:itemMap.keySet()) keys.add(String.valueOf(key));
        }
        if(!keys.isEmpty()) {
            row=addRow(rows,row,new ArrayList<>(keys));
            for(Object item:data) {
                Map<?,?> itemMap=(Map<?,?>)item;List<String> vals=new ArrayList<>();
                for(String key:keys) vals.add(String.valueOf(itemMap.get(key)==null?"":itemMap.get(key)));
                row=addRow(rows,row,vals);
            }
        }
        String sheet="<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+rows+"</sheetData></worksheet>";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){
            entry(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Reporte\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");entry(zip,"xl/worksheets/sheet1.xml",sheet);
        }return bytes.toByteArray();
    }
    private int addRow(StringBuilder out,int n,List<String> values){out.append("<row r=\"").append(n).append("\">");for(int i=0;i<values.size();i++)out.append(cell(i+1,n,values.get(i)));out.append("</row>");return n+1;}
    private String cell(int col,int row,String value){String name="";for(int x=col;x>0;x=(x-1)/26)name=(char)('A'+(x-1)%26)+name;return "<c r=\""+name+row+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+xml(value)+"</t></is></c>";}
    private String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private void entry(ZipOutputStream zip,String name,String value)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}

    private byte[] pdfCompleto(String title,List<String> lines){
        List<String> all=new ArrayList<>();all.addAll(lines);
        List<List<String>> pages=new ArrayList<>();for(int i=0;i<all.size();i+=50)pages.add(all.subList(i,Math.min(i+50,all.size())));if(pages.isEmpty())pages.add(List.of());
        List<String> objects=new ArrayList<>();objects.add("");objects.add("");objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        List<Integer> pageIds=new ArrayList<>();
        for(int i=0;i<pages.size();i++){
            int pageId=4+i*2,contentId=pageId+1;pageIds.add(pageId);
            StringBuilder content=new StringBuilder("BT /F1 14 Tf 45 800 Td (").append(pdfEscape(title+(pages.size()>1?" - "+(i+1)+"/"+pages.size():""))).append(") Tj /F1 9 Tf 0 -24 Td 13 TL\n");
            for(String value:pages.get(i)){String text=value==null?"":value;while(text.length()>115){content.append('(').append(pdfEscape(text.substring(0,115))).append(") Tj T*\n");text=text.substring(115);}content.append('(').append(pdfEscape(text)).append(") Tj T*\n");}
            content.append("ET");objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "+contentId+" 0 R >>");
            objects.add("<< /Length "+content.toString().getBytes(StandardCharsets.ISO_8859_1).length+" >>\nstream\n"+content+"\nendstream");
        }
        String kids=pageIds.stream().map(id->id+" 0 R").reduce((a,b)->a+" "+b).orElse("");objects.set(0,"<< /Type /Catalog /Pages 2 0 R >>");objects.set(1,"<< /Type /Pages /Kids ["+kids+"] /Count "+pages.size()+" >>");
        try{ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));int[] offsets=new int[objects.size()+1];for(int i=0;i<objects.size();i++){offsets[i+1]=out.size();out.write((i+1+" 0 obj\n"+objects.get(i)+"\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));}int xref=out.size();out.write(("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));for(int i=1;i<offsets.length;i++)out.write(String.format(Locale.ROOT,"%010d 00000 n \n",offsets[i]).getBytes(StandardCharsets.ISO_8859_1));out.write(("trailer\n<< /Size "+(objects.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));return out.toByteArray();}catch(Exception e){throw new IllegalStateException("No fue posible generar el PDF del reporte.",e);}
    }
    private String pdfEscape(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?").replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> datosInvalidos(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}
}
