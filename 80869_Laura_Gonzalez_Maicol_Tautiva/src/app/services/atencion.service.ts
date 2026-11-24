import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnotacionClinica } from './historias-clinicas.service';
@Injectable({providedIn:'root'}) export class AtencionService {
  private readonly api='http://localhost:8080/clinica/v1';constructor(private http:HttpClient){}
  iniciar(id:number):Observable<unknown>{return this.http.patch(`${this.api}/atencion-medica/${id}/iniciar`,{});}
  finalizar(id:number,data:{motivo:string;sintomas:string;diagnostico:string;procedimiento:string;observaciones:string;recomendaciones:string}):Observable<AnotacionClinica>{return this.http.post<AnotacionClinica>(`${this.api}/atencion-medica/${id}/finalizar`,data);}
  prefactura(id:number):Observable<Blob>{return this.http.get(`${this.api}/atencion-medica/${id}/prefactura.pdf`,{responseType:'blob'});}
  historialMascota(id:number):Observable<AnotacionClinica[]>{return this.http.get<AnotacionClinica[]>(`${this.api}/historias-clinicas/mascota/${id}/anotaciones`);}
}
