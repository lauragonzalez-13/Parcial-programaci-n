package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportesService {
    private final FacturaRepository facturas;
    private final ConsultaRepository consultas;
    private final MedicamentoRepository medicamentos;
    private final LoteInventarioRepository lotes;
    private final MovimientoInventarioRepository movimientos;

    public ReportesService(FacturaRepository facturas, ConsultaRepository consultas, MedicamentoRepository medicamentos,
            LoteInventarioRepository lotes, MovimientoInventarioRepository movimientos) {
        this.facturas=facturas; this.consultas=consultas; this.medicamentos=medicamentos; this.lotes=lotes; this.movimientos=movimientos;
    }

    @Transactional(readOnly=true)
    public Map<String,Object> generar(String tipo, LocalDate desde, LocalDate hasta, Long veterinarioId, Long especialidadId, Long medicamentoId) {
        validarRango(desde,hasta);
        String categoria=tipo.toUpperCase(Locale.ROOT);
        return switch(categoria) {
            case "FINANCIERO" -> financiero(desde,hasta,veterinarioId,especialidadId);
            case "INVENTARIO" -> inventario(desde,hasta,medicamentoId);
            case "MEDICO" -> medico(desde,hasta,veterinarioId,especialidadId);
            default -> throw new IllegalArgumentException("Tipo de reporte no válido. Usa FINANCIERO, INVENTARIO o MEDICO.");
        };
    }

    private Map<String,Object> financiero(LocalDate desde,LocalDate hasta,Long veterinarioId,Long especialidadId) {
        LocalDateTime inicio=desde.atStartOfDay(), fin=hasta.plusDays(1).atStartOfDay();
        List<Factura> all=facturas.findAllByOrderByFechaGeneracionDesc().stream()
                .filter(f->veterinarioId==null||Objects.equals(f.getVeterinario().getId(),veterinarioId))
                .filter(f->especialidadId==null||f.getVeterinario().getEspecialidad()!=null&&Objects.equals(f.getVeterinario().getEspecialidad().getId(),especialidadId)).toList();
        List<Factura> fs=all.stream()
                .filter(f->!f.getFechaGeneracion().isBefore(inicio)&&f.getFechaGeneracion().isBefore(fin))
                .toList();
        BigDecimal ingresos=BigDecimal.ZERO, ingresoConsultas=BigDecimal.ZERO, ingresoMedicamentos=BigDecimal.ZERO, impuestos=BigDecimal.ZERO, descuentos=BigDecimal.ZERO;
        long pagadas=0, pendientes=0, parciales=0; List<Map<String,Object>> rows=new ArrayList<>(); Map<String,BigDecimal> porVet=new TreeMap<>(),porEsp=new TreeMap<>(),porDia=new TreeMap<>(),porMes=new TreeMap<>(),porAnio=new TreeMap<>();
        for(Factura f:all) {
            String vet=f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido(); String esp=f.getVeterinario().getEspecialidad()==null?"Sin especialidad":f.getVeterinario().getEspecialidad().getNombre();
            for(PagoFactura p:f.getPagos()) if(!p.getFecha().isBefore(inicio)&&p.getFecha().isBefore(fin)) {
                BigDecimal valor=zero(p.getValor()); ingresos=ingresos.add(valor); porVet.merge(vet,valor,BigDecimal::add);porEsp.merge(esp,valor,BigDecimal::add);
                porDia.merge(p.getFecha().toLocalDate().toString(),valor,BigDecimal::add);porMes.merge(p.getFecha().toLocalDate().toString().substring(0,7),valor,BigDecimal::add);porAnio.merge(p.getFecha().toLocalDate().toString().substring(0,4),valor,BigDecimal::add);
            }
        }
        for(Factura f:fs) {
            if(f.getEstado()==EstadoFactura.PAGADA)pagadas++; else if(f.getEstado()==EstadoFactura.PENDIENTE_PAGO)pendientes++; else if(f.getEstado()==EstadoFactura.PAGO_PARCIAL)parciales++;
            if(f.getEstado()!=EstadoFactura.ANULADA){descuentos=descuentos.add(zero(f.getDescuento())); impuestos=impuestos.add(zero(f.getImpuestos()));}
            BigDecimal consulta=BigDecimal.ZERO, meds=BigDecimal.ZERO;
            for(LineaFactura l:f.getEstado()==EstadoFactura.ANULADA?List.<LineaFactura>of():f.getLineas()) {
                String t=Optional.ofNullable(l.getTipo()).orElse("").toUpperCase(Locale.ROOT);
                if(t.contains("MEDIC")||t.contains("PRODUCT")||t.contains("INVENT"))meds=meds.add(zero(l.getTotal())); else consulta=consulta.add(zero(l.getTotal()));
            }
            ingresoConsultas=ingresoConsultas.add(consulta); ingresoMedicamentos=ingresoMedicamentos.add(meds);
            String vet=f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido(); String esp=f.getVeterinario().getEspecialidad()==null?"Sin especialidad":f.getVeterinario().getEspecialidad().getNombre();
            BigDecimal pagadoPeriodo=BigDecimal.ZERO;
            for(PagoFactura p:f.getPagos()) if(!p.getFecha().isBefore(inicio)&&p.getFecha().isBefore(fin)) pagadoPeriodo=pagadoPeriodo.add(zero(p.getValor()));
            rows.add(row("fecha",f.getFechaGeneracion(),"factura",f.getNumero(),"propietario",f.getCliente().getNombre()+" "+f.getCliente().getApellido(),"mascota",f.getMascota().getNombre(),"veterinario",vet,"especialidad",esp,"estado",f.getEstado(),"total",f.getTotal(),"pagadoEnPeriodo",pagadoPeriodo,"impuestos",f.getImpuestos(),"descuento",f.getDescuento()));
        }
        porDia.forEach((k,v)->rows.add(row("tipo","Ingresos por día","grupo",k,"total",v)));
        porMes.forEach((k,v)->rows.add(row("tipo","Ingresos por mes","grupo",k,"total",v)));
        porAnio.forEach((k,v)->rows.add(row("tipo","Ingresos por año","grupo",k,"total",v)));
        porVet.forEach((k,v)->rows.add(row("tipo","Ingresos por veterinario","grupo",k,"total",v)));
        porEsp.forEach((k,v)->rows.add(row("tipo","Ingresos por especialidad","grupo",k,"total",v)));
        return report("Reporte financiero",desde,hasta,row("ingresosPorPagos",ingresos,"ingresosPorConsultasFacturadas",ingresoConsultas,"ingresosPorProductosFacturados",ingresoMedicamentos,"facturasGeneradas",fs.size(),"facturasPagadas",pagadas,"facturasPendientes",pendientes,"pagosParciales",parciales,"impuestosRegistrados",impuestos,"descuentosAplicados",descuentos),rows);
    }

    private Map<String,Object> inventario(LocalDate desde,LocalDate hasta,Long medicamentoId) {
        LocalDate hoy=LocalDate.now(); List<Medicamento> productos=medicamentos.findAll().stream().filter(Medicamento::isActivo).filter(m->medicamentoId==null||Objects.equals(m.getId(),medicamentoId)).toList();
        List<LoteInventario> all=lotes.findAll(); List<Map<String,Object>> rows=new ArrayList<>(); BigDecimal costo=BigDecimal.ZERO,venta=BigDecimal.ZERO; int agotados=0,minimos=0,proximos=0,vencidos=0;
        for(Medicamento m:productos) {
            List<LoteInventario> ml=all.stream().filter(l->Objects.equals(l.getMedicamento().getId(),m.getId())).toList();
            BigDecimal stock=ml.stream().map(LoteInventario::getCantidadDisponible).map(ReportesService::zero).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal valorCosto=ml.stream().map(l->zero(l.getCantidadDisponible()).multiply(zero(l.getPrecioCompra()))).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal valorVenta=ml.stream().map(l->zero(l.getCantidadDisponible()).multiply(zero(l.getPrecioVenta()))).reduce(BigDecimal.ZERO,BigDecimal::add); costo=costo.add(valorCosto);venta=venta.add(valorVenta);
            boolean agotado=stock.signum()==0, minimo=stock.signum()>0&&stock.compareTo(zero(m.getStockMinimo()))<=0;
            long vencidosProducto=ml.stream().filter(l->l.getFechaVencimiento()!=null&&l.getFechaVencimiento().isBefore(hoy)&&l.getCantidadDisponible().signum()>0).count();
            long proximosProducto=ml.stream().filter(l->l.getFechaVencimiento()!=null&&!l.getFechaVencimiento().isBefore(hoy)&&!l.getFechaVencimiento().isAfter(hoy.plusDays(30))&&l.getCantidadDisponible().signum()>0).count();
            if(agotado)agotados++;if(minimo)minimos++;if(vencidosProducto>0)vencidos++;if(proximosProducto>0)proximos++;
            rows.add(row("tipo","Existencia","medicamento",m.getNombre(),"codigo",m.getCodigo(),"stock",stock,"stockMinimo",m.getStockMinimo(),"valorCosto",valorCosto,"valorVentaPotencial",valorVenta,"agotado",agotado,"stockMinimoAlcanzado",minimo,"lotesProximosVencer",proximosProducto,"lotesVencidos",vencidosProducto));
        }
        for(MovimientoInventario mov:movimientos.findAllByOrderByFechaDesc()) if((medicamentoId==null||Objects.equals(mov.getMedicamento().getId(),medicamentoId))&&!mov.getFecha().toLocalDate().isBefore(desde)&&!mov.getFecha().toLocalDate().isAfter(hasta)) rows.add(row("tipo","Movimiento","fecha",mov.getFecha(),"medicamento",mov.getMedicamento().getNombre(),"lote",mov.getLote().getCodigoLote(),"movimiento",mov.getTipo(),"cantidad",mov.getCantidad(),"responsable",mov.getUsuarioResponsable(),"observacion",mov.getObservacion()));
        return report("Reporte de inventario",desde,hasta,row("productos",productos.size(),"valorCostoAdquisicion",costo,"valorPotencialVenta",venta,"medicamentosAgotados",agotados,"medicamentosConStockMinimo",minimos,"medicamentosProximosVencer",proximos,"medicamentosVencidos",vencidos),rows);
    }

    private Map<String,Object> medico(LocalDate desde,LocalDate hasta,Long veterinarioId,Long especialidadId) {
        LocalDateTime inicio=desde.atStartOfDay(),fin=hasta.plusDays(1).atStartOfDay();
        List<Consulta> cs=consultas.findAll().stream().filter(c->!c.getFechaHora().isBefore(inicio)&&c.getFechaHora().isBefore(fin))
                .filter(c->veterinarioId==null||Objects.equals(c.getVeterinario().getId(),veterinarioId))
                .filter(c->especialidadId==null||c.getVeterinario().getEspecialidad()!=null&&Objects.equals(c.getVeterinario().getEspecialidad().getId(),especialidadId)).toList();
        long atendidas=cs.stream().filter(c->c.getEstado()==EstadoConsulta.ATENDIDA).count(),canceladas=cs.stream().filter(c->c.getEstado()==EstadoConsulta.CANCELADA).count(),pendientes=cs.stream().filter(c->c.getEstado()==EstadoConsulta.PENDIENTE||c.getEstado()==EstadoConsulta.CONFIRMADA||c.getEstado()==EstadoConsulta.PROGRAMADA).count();
        Set<Long> mascotas=new HashSet<>();List<Map<String,Object>> rows=new ArrayList<>();Map<String,Long> vetCount=new TreeMap<>(),espCount=new TreeMap<>();
        for(Consulta c:cs){String vet=c.getVeterinario().getNombre()+" "+c.getVeterinario().getApellido();String esp=c.getVeterinario().getEspecialidad()==null?"Sin especialidad":c.getVeterinario().getEspecialidad().getNombre();if(c.getEstado()==EstadoConsulta.ATENDIDA){mascotas.add(c.getMascota().getId());vetCount.merge(vet,1L,Long::sum);espCount.merge(esp,1L,Long::sum);}rows.add(row("tipo","Cita","fecha",c.getFechaHora(),"mascota",c.getMascota().getNombre(),"propietario",c.getMascota().getCliente().getNombre()+" "+c.getMascota().getCliente().getApellido(),"veterinario",vet,"especialidad",esp,"motivo",c.getMotivo(),"estado",c.getEstado()));}
        vetCount.forEach((k,v)->rows.add(row("tipo","Consultas por veterinario","grupo",k,"cantidad",v)));espCount.forEach((k,v)->rows.add(row("tipo","Consultas por especialidad","grupo",k,"cantidad",v)));
        return report("Reporte de gestión médica",desde,hasta,row("citas",cs.size(),"citasAtendidas",atendidas,"citasCanceladas",canceladas,"citasPendientes",pendientes,"mascotasAtendidas",mascotas.size()),rows);
    }

    private static BigDecimal zero(BigDecimal n){return n==null?BigDecimal.ZERO:n;}
    private static void validarRango(LocalDate desde,LocalDate hasta){if(desde==null||hasta==null)throw new IllegalArgumentException("Debes indicar las fechas inicial y final.");if(hasta.isBefore(desde))throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");}
    private static Map<String,Object> row(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put(String.valueOf(pairs[i]),pairs[i+1]);return m;}
    private static Map<String,Object> report(String titulo,LocalDate desde,LocalDate hasta,Map<String,Object> totales,List<Map<String,Object>> filas){return row("tipo",titulo,"desde",desde,"hasta",hasta,"generado",LocalDateTime.now(),"totales",totales,"filas",filas);}
}
