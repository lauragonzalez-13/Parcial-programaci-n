import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// project import
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';
import { perfilRutaGuard } from './services/perfil-ruta.guard';
import { authRutaGuard } from './services/auth-ruta.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    canActivate: [authRutaGuard],
    canActivateChild: [perfilRutaGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./demo/dashboard/dashboard.component').then((c) => c.DashboardComponent)
      },
      {
        path: 'pacientes',
        loadComponent: () => import('./demo/pages/pacientes/mascotas').then((c) => c.MascotasComponent)
      },
      {
        path: 'formulas-medicas',
        loadComponent: () => import('./demo/pages/pacientes/pacientes').then((c) => c.PacientesComponent)
      },
      {
        path: 'inventario',
        loadComponent: () => import('./demo/pages/inventario').then((c) => c.InventarioComponent)
      },
      {
        path: 'despacho-medico',
        canActivate: [perfilRutaGuard],
        loadComponent: () => import('./demo/pages/despacho-medico').then((c) => c.DespachoMedicoComponent)
      },
      {
        path: 'citas',
        canActivate: [perfilRutaGuard], data: { perfil: 'NO_VETERINARIO' },
        loadComponent: () => import('./demo/pages/citas/citas').then((c) => c.CitasComponent)
      },
      {
        path: 'calendario-medico',
        canActivate: [perfilRutaGuard], data: { perfil: 'VETERINARIO' },
        loadComponent: () => import('./demo/pages/calendario-medico').then((c) => c.CalendarioMedicoComponent)
      },
      {
        path: 'disponibilidad-veterinarios',
        canActivate: [perfilRutaGuard], data: { perfil: 'NO_VETERINARIO' },
        loadComponent: () => import('./demo/pages/disponibilidad-veterinarios').then((c) => c.DisponibilidadVeterinariosComponent)
      },
      {
        path: 'atencion-medica/:id',
        loadComponent: () => import('./demo/pages/atencion-medica').then((c) => c.AtencionMedicaComponent)
      },
      {
        path: 'tarifas-especialidades',
        canActivate: [perfilRutaGuard], data: { perfil: 'ADMINISTRADOR' },
        loadComponent: () => import('./demo/pages/tarifas-especialidades').then((c) => c.TarifasEspecialidadesComponent)
      },
      {
        path: 'usuarios',
        canActivate: [perfilRutaGuard], data: { perfil: 'ADMINISTRADOR' },
        loadComponent: () => import('./demo/pages/usuarios').then((c) => c.UsuariosComponent)
      },
      {
        path: 'facturacion',
        canActivate: [perfilRutaGuard],
        loadComponent: () => import('./demo/pages/facturacion').then((c) => c.FacturacionComponent)
      },
      {
        path: 'reportes-administrativos',
        canActivate: [perfilRutaGuard], data: { perfil: 'NO_VETERINARIO' },
        loadComponent: () => import('./demo/pages/reportes-administrativos').then((c) => c.ReportesAdministrativosComponent)
      },
      {
        path: 'historias-clinicas',
        loadComponent: () => import('./demo/pages/historias-clinicas/historias-clinicas').then((c) => c.HistoriasClinicasComponent)
      },
      {
        path: 'basic',
        loadChildren: () => import('./demo/ui-elements/ui-basic/ui-basic.module').then((m) => m.UiBasicModule)
      },
      {
        path: 'forms',
        loadComponent: () => import('./demo/pages/form-element/form-element').then((c) => c.FormElement)
      },
      {
        path: 'tables',
        loadComponent: () => import('./demo/pages/tables/tbl-bootstrap/tbl-bootstrap.component').then((c) => c.TblBootstrapComponent)
      },
      {
        path: 'apexchart',
        loadComponent: () => import('./demo/pages/core-chart/apex-chart/apex-chart.component').then((c) => c.ApexChartComponent)
      },
      {
        path: 'sample-page',
        loadComponent: () => import('./demo/extra/sample-page/sample-page.component').then((c) => c.SamplePageComponent)
      }
    ]
  },
  {
    path: '',
    component: GuestComponent,
    children: [
      {
        path: 'login',
        loadComponent: () => import('./demo/pages/authentication/auth-signin/auth-signin.component').then((c) => c.AuthSigninComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./demo/pages/authentication/auth-signup/auth-signup.component').then((c) => c.AuthSignupComponent)
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
