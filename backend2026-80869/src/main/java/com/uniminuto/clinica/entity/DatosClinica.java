package com.uniminuto.clinica.entity;
import jakarta.persistence.*;
@Entity @Table(name="datos_clinica")
public class DatosClinica {
    @Id private Long id=1L;
    @Column(nullable=false,length=140) private String nombre="Clínica veterinaria";
    @Column(length=30) private String nit; @Column(length=30) private String telefono; @Column(length=200) private String direccion; @Column(length=150) private String correo;
    public Long getId(){return id;}public String getNombre(){return nombre;}public void setNombre(String v){nombre=v;}public String getNit(){return nit;}public void setNit(String v){nit=v;}public String getTelefono(){return telefono;}public void setTelefono(String v){telefono=v;}public String getDireccion(){return direccion;}public void setDireccion(String v){direccion=v;}public String getCorreo(){return correo;}public void setCorreo(String v){correo=v;}
}
