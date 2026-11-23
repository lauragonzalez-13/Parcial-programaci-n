export interface NavigationItem {
  id: string;
  title: string;
  type: 'item' | 'collapse' | 'group';
  translate?: string;
  icon?: string;
  hidden?: boolean;
  url?: string;
  classes?: string;
  exactMatch?: boolean;
  external?: boolean;
  target?: boolean;
  breadcrumbs?: boolean;
  children?: NavigationItem[];
}
export const NavigationItems: NavigationItem[] = [
  {
    id: 'navigation',
    title: 'Navigation',
    type: 'group',
    icon: 'icon-navigation',
    children: [
      {
        id: 'dashboard',
        title: 'Dashboard',
        type: 'item',
        url: '/dashboard',
        icon: 'feather icon-home',
        classes: 'nav-item'
      }
    ]
  },
  {
    id: 'pacientes',
    title: 'Gestión de pacientes',
    type: 'group',
    icon: 'icon-activity',
    children: [{ id: 'pacientes-listado', title: 'Pacientes', type: 'item', url: '/pacientes', icon: 'icon-activity', classes: 'nav-item' }]
  },
  {
    id: 'cita_medica',
    title: 'Gestión de Citas Médicas',
    type: 'group',
    icon: 'icon-activity',
    children: [
      {
        id: 'cita',
        title: 'Citás Médicas',
        type: 'item',
        url: '/citas',
        icon: 'icon-activity',
        classes: 'nav-item'
      },
      {
        id: 'disponibilidad-veterinarios',
        title: 'Disponibilidad de veterinarios',
        type: 'item',
        url: '/disponibilidad-veterinarios',
        icon: 'icon-clock',
        classes: 'nav-item'
      }
    ]
  },
  {
    id: 'historial_medico',
    title: 'Gestión de Historial Médico',
    type: 'group',
    icon: 'icon-activity',
    children: [
      {
        id: 'historias',
        title: 'Historias clinicas y anotaciones',
        type: 'item',
        url: '/historias-clinicas',
        icon: 'icon-activity',
        classes: 'nav-item'
      }
    ]
  },
  {
    id: 'formula_medica',
    title: 'Gestión de Fórmulas Médica',
    type: 'group',
    icon: 'icon-activity',
    children: [
      {
        id: 'formula',
        title: 'Formula Médica',
        type: 'item',
        url: '/formulas-medicas',
        icon: 'icon-activity',
        classes: 'nav-item'
      }
    ]
  },
  {
    id: 'inventario',
    title: 'Inventario',
    type: 'group',
    icon: 'icon-box',
    children: [{ id: 'inventario-productos', title: 'Medicamentos y productos', type: 'item', url: '/inventario', icon: 'icon-box', classes: 'nav-item' }]
  },
  {
    id: 'admin_tarifas', title: 'Administración clínica', type: 'group', icon: 'icon-settings',
    children: [{ id: 'tarifas-especialidades', title: 'Especialidades y tarifas', type: 'item', url: '/tarifas-especialidades', icon: 'icon-settings', classes: 'nav-item' }]
  },
  {
    id: 'admin_usuarios', title: 'Administración de acceso', type: 'group', icon: 'icon-user',
    children: [{ id: 'usuarios', title: 'Usuarios y perfiles', type: 'item', url: '/usuarios', icon: 'icon-user', classes: 'nav-item' }]
  },
  {
    id: 'reportes', title: 'Reportes', type: 'group', icon: 'icon-bar-chart',
    children: [{ id: 'reportes-administrativos', title: 'Reportes administrativos', type: 'item', url: '/reportes-administrativos', icon: 'icon-bar-chart', classes: 'nav-item' }]
  },
  {
    id: 'despacho_medico', title: 'Despacho médico', type: 'group', icon: 'icon-box',
    children: [{ id: 'despachos-pendientes', title: 'Despacho de fórmulas', type: 'item', url: '/despacho-medico', icon: 'icon-box', classes: 'nav-item' }]
  },
  {
    id: 'facturacion', title: 'Facturación', type: 'group', icon: 'icon-credit-card',
    children: [{ id: 'facturas-listado', title: 'Facturas y pagos', type: 'item', url: '/facturacion', icon: 'icon-credit-card', classes: 'nav-item' }]
  },
  {
    id: 'chart-maps',
    title: 'Chart',
    type: 'group',
    icon: 'icon-charts',
    children: [
      {
        id: 'apexChart',
        title: 'ApexChart',
        type: 'item',
        url: 'apexchart',
        classes: 'nav-item',
        icon: 'feather icon-pie-chart'
      }
    ]
  },
  {
    id: 'pages',
    title: 'Pages',
    type: 'group',
    icon: 'icon-pages',
    children: [
      {
        id: 'auth',
        title: 'Authentication',
        type: 'collapse',
        icon: 'feather icon-lock',
        children: [
          {
            id: 'signup',
            title: 'Sign up',
            type: 'item',
            url: '/register',
            target: true,
            breadcrumbs: false
          },
          {
            id: 'signin',
            title: 'Sign in',
            type: 'item',
            url: '/login',
            target: true,
            breadcrumbs: false
          }
        ]
      },
      {
        id: 'sample-page',
        title: 'Sample Page',
        type: 'item',
        url: '/sample-page',
        classes: 'nav-item',
        icon: 'feather icon-sidebar'
      },
      {
        id: 'disabled-menu',
        title: 'Disabled Menu',
        type: 'item',
        url: 'javascript:void(0)',
        classes: 'nav-item disabled',
        icon: 'feather icon-power',
        external: true
      },
      {
        id: 'buy_now',
        title: 'Buy Now',
        type: 'item',
        icon: 'feather icon-book',
        classes: 'nav-item',
        url: 'https://codedthemes.com/item/datta-able-angular/',
        target: true,
        external: true
      }
    ]
  }
];
