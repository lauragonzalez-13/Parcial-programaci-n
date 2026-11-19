import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService, RolClinica, UsuarioSistema } from 'src/app/services/auth.service';
import { ConsultasService, VeterinarioCita } from 'src/app/services/consultas.service';

@Component({ selector: 'app-usuarios', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './usuarios.html', styleUrl: './usuarios.scss' })
export class UsuariosComponent implements OnInit {
  usuarios: UsuarioSistema[] = []; veterinarios: VeterinarioCita[] = [];
  usuarioActualId = Number(localStorage.getItem('clinica.usuarioId') || 0);
  username = ''; password = ''; rol: RolClinica = 'SECRETARIA'; veterinarioId: number | null = null;
  error = ''; mensaje = ''; guardando = false;
  constructor(private auth: AuthService, private consultas: ConsultasService, private cdr: ChangeDetectorRef) {}
  ngOnInit(): void { this.cargar(); this.consultas.veterinarios().subscribe({next: vets => { this.veterinarios = vets.filter(v => v.activo); this.cdr.detectChanges(); }}); }
  cargar(): void { this.auth.usuarios().subscribe({next: rows => { this.usuarios = rows; this.cdr.detectChanges(); }, error: e => { this.error = e?.error?.mensaje || 'No se pudieron consultar los usuarios.'; this.cdr.detectChanges(); }}); }
  crear(): void {
    this.error = ''; this.mensaje = '';
    if (this.username.trim().length < 3 || this.password.length < 8) { this.error = 'Usa un username de al menos 3 caracteres y una contraseña de al menos 8.'; return; }
    if (this.rol === 'VETERINARIO' && !this.veterinarioId) { this.error = 'Selecciona el veterinario asociado a la cuenta.'; return; }
    this.guardando = true;
    this.auth.crearUsuario({username:this.username.trim(), password:this.password, rol:this.rol, veterinarioId:this.rol==='VETERINARIO'?this.veterinarioId:null})
      .subscribe({next: user => { this.mensaje = `Cuenta ${user.username} creada con rol ${user.rol}.`; this.username=''; this.password=''; this.rol='SECRETARIA'; this.veterinarioId=null; this.guardando=false; this.cargar(); this.cdr.detectChanges(); }, error: e => { this.error=e?.error?.mensaje||'No se pudo crear la cuenta.'; this.guardando=false; this.cdr.detectChanges(); }});
  }
  eliminar(usuario: UsuarioSistema): void {
    if (!confirm(`¿Eliminar la cuenta ${usuario.username} con rol ${usuario.rol}?`)) return;
    this.error = ''; this.mensaje = '';
    this.auth.eliminarUsuario(usuario.usuarioId).subscribe({
      next: () => { this.mensaje = `La cuenta ${usuario.username} fue eliminada.`; this.cargar(); },
      error: e => { this.error = e?.error?.mensaje || 'No se pudo eliminar la cuenta.'; this.cdr.detectChanges(); }
    });
  }
}
