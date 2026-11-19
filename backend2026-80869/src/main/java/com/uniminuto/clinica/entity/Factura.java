package com.uniminuto.clinica.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="factura",uniqueConstraints={@UniqueConstraint(name="uk_factura_consulta",columnNames="consulta_id"),@UniqueConstraint(name="uk_factura_numero",columnNames="numero")})
public class Factura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(length=30,unique=true) private String numero;
    @OneToOne(optional=false) @JoinColumn(name="consulta_id",nullable=false,unique=true) private Consulta consulta;
    @ManyToOne(optional=false) @JoinColumn(name="cliente_id",nullable=false) private Cliente cliente;
    @ManyToOne(optional=false) @JoinColumn(name="mascota_id",nullable=false) private Mascota mascota;
    @ManyToOne(optional=false) @JoinColumn(name="veterinario_id",nullable=false) private Veterinario veterinario;
    @Column(nullable=false) private LocalDateTime fechaGeneracion=LocalDateTime.now();
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private EstadoFactura estado=EstadoFactura.PENDIENTE_PAGO;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal=BigDecimal.ZERO;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal descuento=BigDecimal.ZERO;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal impuestos=BigDecimal.ZERO;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal total=BigDecimal.ZERO;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal pagado=BigDecimal.ZERO;
    @Column(length=160) private String descuentoAutorizadoPor;
    @Column(length=500) private String motivoAnulacion;
    @Column(length=120) private String anuladaPor;
    @Column private LocalDateTime fechaAnulacion;
    @OneToMany(mappedBy="factura",cascade=CascadeType.ALL,orphanRemoval=true) private List<LineaFactura> lineas=new ArrayList<>();
    @OneToMany(mappedBy="factura",cascade=CascadeType.ALL) private List<PagoFactura> pagos=new ArrayList<>();
    @OneToMany(mappedBy="factura",cascade=CascadeType.ALL) private List<EventoFactura> eventos=new ArrayList<>();
    public void agregarLinea(LineaFactura l){l.setFactura(this);lineas.add(l);}
    public void agregarPago(PagoFactura p){p.setFactura(this);pagos.add(p);}
    public void agregarEvento(EventoFactura e){e.setFactura(this);eventos.add(e);}
    public Long getId(){return id;} public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
    public Consulta getConsulta(){return consulta;} public void setConsulta(Consulta v){consulta=v;} public Cliente getCliente(){return cliente;} public void setCliente(Cliente v){cliente=v;}
    public Mascota getMascota(){return mascota;} public void setMascota(Mascota v){mascota=v;} public Veterinario getVeterinario(){return veterinario;} public void setVeterinario(Veterinario v){veterinario=v;}
    public LocalDateTime getFechaGeneracion(){return fechaGeneracion;} public EstadoFactura getEstado(){return estado;} public void setEstado(EstadoFactura v){estado=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;} public BigDecimal getDescuento(){return descuento;} public void setDescuento(BigDecimal v){descuento=v;}
    public BigDecimal getImpuestos(){return impuestos;} public void setImpuestos(BigDecimal v){impuestos=v;} public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
    public BigDecimal getPagado(){return pagado;} public void setPagado(BigDecimal v){pagado=v;} public String getDescuentoAutorizadoPor(){return descuentoAutorizadoPor;} public void setDescuentoAutorizadoPor(String v){descuentoAutorizadoPor=v;}
    public String getMotivoAnulacion(){return motivoAnulacion;} public void setMotivoAnulacion(String v){motivoAnulacion=v;} public String getAnuladaPor(){return anuladaPor;} public void setAnuladaPor(String v){anuladaPor=v;}
    public LocalDateTime getFechaAnulacion(){return fechaAnulacion;} public void setFechaAnulacion(LocalDateTime v){fechaAnulacion=v;}
    public List<LineaFactura> getLineas(){return lineas;} public List<PagoFactura> getPagos(){return pagos;} public List<EventoFactura> getEventos(){return eventos;}
}
