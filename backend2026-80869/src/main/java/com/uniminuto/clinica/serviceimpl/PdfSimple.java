package com.uniminuto.clinica.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

/** Creates a small printable one-page PDF without an external library. */
public final class PdfSimple {
    private PdfSimple() { }
    public static byte[] crear(String titulo,List<String> lineas){
        StringBuilder content=new StringBuilder("BT /F1 15 Tf 50 790 Td (").append(escapar(titulo)).append(") Tj /F1 10 Tf 0 -28 Td 13 TL\n");
        int n=0;
        for(String linea:lineas){String value=linea==null?"":linea;while(value.length()>105){if(n++>46)break;content.append('(').append(escapar(value.substring(0,105))).append(") Tj T*\n");value=value.substring(105);}if(n++>46)break;content.append('(').append(escapar(value)).append(") Tj T*\n");}
        content.append("ET");
        String stream=content.toString();
        String[] objects={"<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>","<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>","<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>","<< /Length "+stream.getBytes(StandardCharsets.ISO_8859_1).length+" >>\nstream\n"+stream+"\nendstream"};
        try{ByteArrayOutputStream out=new ByteArrayOutputStream();write(out,"%PDF-1.4\n");int[] offsets=new int[objects.length+1];for(int i=0;i<objects.length;i++){offsets[i+1]=out.size();write(out,(i+1)+" 0 obj\n"+objects[i]+"\nendobj\n");}int xref=out.size();write(out,"xref\n0 "+(objects.length+1)+"\n0000000000 65535 f \n");for(int i=1;i<offsets.length;i++)write(out,String.format("%010d 00000 n \n",offsets[i]));write(out,"trailer\n<< /Size "+(objects.length+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF");return out.toByteArray();}catch(Exception e){throw new IllegalStateException("No fue posible generar el PDF.",e);}
    }
    private static String escapar(String s){String a=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?");return a.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}
    private static void write(ByteArrayOutputStream out,String s){out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));}
}
