// angular import
import { Component, inject, output } from '@angular/core';
import { Location } from '@angular/common';

// project import
import { environment } from 'src/environments/environment';
import { NavigationItem, NavigationItems } from '../navigation';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NavGroupComponent } from './nav-group/nav-group.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConsultasService, VeterinarioCita } from 'src/app/services/consultas.service';
import { PerfilClinica, PerfilDemoService } from 'src/app/services/perfil-demo.service';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-nav-content',
  imports: [SharedModule, NavGroupComponent, CommonModule, FormsModule],
  templateUrl: './nav-content.component.html',
  styleUrls: ['./nav-content.component.scss']
})
export class NavContentComponent {
  private location = inject(Location);

  // public method
  // version
  title = 'Demo application for version numbering';
  currentApplicationVersion = environment.appVersion;

  navigations!: NavigationItem[];
  wrapperWidth: number;
  windowWidth = window.innerWidth;
  perfilService = inject(PerfilDemoService);
  auth = inject(AuthService);
  consultas = inject(ConsultasService);
  router = inject(Router);
  veterinarios: VeterinarioCita[] = [];
  perfil: PerfilClinica = this.perfilService.perfil;
  username = this.auth.username;
  veterinarioId = this.perfilService.veterinarioId;

  NavCollapsedMob = output();

  // constructor
  constructor() {
    this.navigations = structuredClone(NavigationItems);
    if(this.perfil==='VETERINARIO')this.consultas.veterinarios().subscribe({ next: vets => { this.veterinarios = vets.filter(v => v.activo); if (!this.veterinarioId && this.veterinarios.length) { this.veterinarioId = this.veterinarios[0].id; this.perfilService.veterinarioId = this.veterinarioId; } } });
    this.actualizarMenu();
  }

  cambiarVeterinario(): void {
    this.perfilService.veterinarioId = Number(this.veterinarioId);
    void this.router.navigateByUrl('/calendario-medico');
  }
  cerrarSesion(): void { this.auth.cerrarSesion(); }
  private actualizarMenu(): void {
    // Rebuild the navigation tree from the authenticated user's role.
    this.navigations = structuredClone(NavigationItems);
    const group = this.navigations?.find(x => x.id === 'cita_medica');
    const item = group?.children?.[0];
    if (item) { item.url = this.perfil === 'VETERINARIO' ? '/calendario-medico' : '/citas'; item.title = this.perfil === 'VETERINARIO' ? 'Calendario médico' : 'Citas médicas'; }
    const disponibilidad = group?.children?.find(x => x.id === 'disponibilidad-veterinarios');
    if (disponibilidad) disponibilidad.hidden = this.perfil === 'VETERINARIO';
    const adminGroup = this.navigations?.find(x => x.id === 'admin_tarifas');
    if (adminGroup) adminGroup.hidden = this.perfil !== 'ADMINISTRADOR';
    const usersGroup = this.navigations?.find(x => x.id === 'admin_usuarios');
    if (usersGroup) usersGroup.hidden = this.perfil !== 'ADMINISTRADOR';
    const billingGroup = this.navigations?.find(x => x.id === 'facturacion');
    if (billingGroup) billingGroup.hidden = this.perfil === 'DESPACHO_MEDICO';
    const reportsGroup = this.navigations?.find(x => x.id === 'reportes');
    if (reportsGroup) reportsGroup.hidden = this.perfil === 'VETERINARIO';
    const despachoGroup = this.navigations?.find(x => x.id === 'despacho_medico');
    if (despachoGroup) despachoGroup.hidden = !['DESPACHO_MEDICO', 'ADMINISTRADOR', 'VETERINARIO', 'SECRETARIA'].includes(this.perfil);
    if (this.perfil === 'DESPACHO_MEDICO') {
      for (const navItem of this.navigations) navItem.hidden = !['inventario', 'despacho_medico'].includes(navItem.id);
    }
  }

  fireOutClick() {
    let current_url = this.location.path();
    if (this.location['_baseHref']) {
      current_url = this.location['_baseHref'] + this.location.path();
    }
    const link = "a.nav-link[ href='" + current_url + "' ]";
    const ele = document.querySelector(link);
    if (ele !== null && ele !== undefined) {
      const parent = ele.parentElement;
      const up_parent = parent.parentElement.parentElement;
      const last_parent = up_parent.parentElement;
      if (parent.classList.contains('pcoded-hasmenu')) {
        parent.classList.add('pcoded-trigger');
        parent.classList.add('active');
      } else if (up_parent.classList.contains('pcoded-hasmenu')) {
        up_parent.classList.add('pcoded-trigger');
        up_parent.classList.add('active');
      } else if (last_parent.classList.contains('pcoded-hasmenu')) {
        last_parent.classList.add('pcoded-trigger');
        last_parent.classList.add('active');
      }
    }
  }
}
