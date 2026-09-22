import { Component, computed, inject } from '@angular/core';
import { ModuleConfig } from '../../core/models/module-config.models';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matReceiptLongOutline,
  matInventory2Outline,
  matGroupOutline,
  matTuneOutline,
  matHourglassTopOutline,
  matCheckCircleOutline,
  matArrowBackOutline,
  matStorefrontOutline,
  matViewWeekOutline,
  matHomeOutline,
  matScheduleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matReceiptLongOutline,
      matInventory2Outline,
      matGroupOutline,
      matTuneOutline,
      matHourglassTopOutline,
      matCheckCircleOutline,
      matArrowBackOutline,
      matStorefrontOutline,
      matViewWeekOutline,
      matHomeOutline,
      matScheduleOutline,
    }),
  ],
  selector: 'app-coming-soon',
  styleUrl: './coming-soon.css',
  templateUrl: './coming-soon.html',
})
export class ComingSoon {
  private readonly route = inject(ActivatedRoute);

  readonly config = computed<ModuleConfig>(() => {
    const dataKey = this.route.snapshot.data?.['moduleKey'];
    if (dataKey && this.MODULE_CONFIGS[dataKey]) {
      return this.MODULE_CONFIGS[dataKey];
    }

    const urlSegments = this.route.snapshot.url;
    const path = urlSegments.length > 0 ? urlSegments[0].path : '';
    return this.MODULE_CONFIGS[path] || this.DEFAULT_CONFIG;
  });

  MODULE_CONFIGS: Record<string, ModuleConfig> = {
    products: {
      key: 'products',
      title: 'Catálogo y Productos',
      subtitle: 'Gestión integral de categorías, productos, variantes y stock.',
      description:
        'Este módulo te permitirá crear tu catálogo digital, definir precios diferenciados por variante, configurar opciones de presentación y controlar existencias con alertas de stock bajo.',
      sprint: 'Sprint 3 · En Desarrollo Activo',
      sprintBadgeClass: 'bg-primary-fixed/60 text-primary-container border-primary-container/30',
      iconName: 'matInventory2Outline',
      iconBgClass: 'bg-primary-fixed/40 border-primary-container/20',
      iconTextClass: 'text-primary-container',
      features: [
        {
          title: 'Categorías y Variantes',
          desc: 'Organiza por familias y agrega variantes (tamaño, peso, sabor) con SKU único.',
        },
        {
          title: 'Control de Inventario',
          desc: 'Define existencias por variante y umbrales mínimos de stock para reposición.',
        },
        {
          title: 'Visibilidad en Tienda',
          desc: 'Activa o pausa productos en tiempo real para tu vitrina digital pública.',
        },
      ],
      eta: 'Sprint 3 (Próxima entrega)',
    },
    orders: {
      key: 'orders',
      title: 'Gestión de Pedidos en Vivo',
      subtitle: 'Recepción, seguimiento y ciclo de vida de pedidos en tiempo real.',
      description:
        'Aquí podrás visualizar los pedidos entrantes sincronizados con tus franjas de capacidad, cambiar estados operativos (Pendiente, En Preparación, Listo, Entregado) y gestionar comprobantes.',
      sprint: 'Sprint 4 · Próximo Desbloqueo',
      sprintBadgeClass: 'bg-secondary-fixed/60 text-secondary border-secondary/30',
      iconName: 'matReceiptLongOutline',
      iconBgClass: 'bg-secondary-fixed/40 border-secondary/20',
      iconTextClass: 'text-secondary',
      features: [
        {
          title: 'Tablero de Control Operativo',
          desc: 'Visualiza órdenes filtradas por día y franja horaria de despacho.',
        },
        {
          title: 'Estados Transaccionales',
          desc: 'Actualiza el progreso del pedido y notifica al cliente en cada hito.',
        },
        {
          title: 'Validación de Capacidad',
          desc: 'Cada orden confirmada descuenta automáticamente un cupo de tu capacidad real.',
        },
      ],
      eta: 'Sprint 4 (Checkout & Pedidos)',
    },
    customers: {
      key: 'customers',
      title: 'Directorio de Clientes',
      subtitle: 'Historial de compras, recurrencia y datos de contacto frecuente.',
      description:
        'Conoce a tus compradores habituales, consulta sus pedidos pasados, preferencias de entrega y comunícate directamente por WhatsApp con un solo clic.',
      sprint: 'Sprint 4 · Base de Clientes',
      sprintBadgeClass:
        'bg-surface-container-high text-on-surface-variant border-outline-variant/40',
      iconName: 'matGroupOutline',
      iconBgClass: 'bg-surface-container-high border-outline-variant/30',
      iconTextClass: 'text-on-surface',
      features: [
        {
          title: 'Historial por Cliente',
          desc: 'Consulta todas las compras realizadas por un cliente y su ticket promedio.',
        },
        {
          title: 'Contacto Inmediato',
          desc: 'Acceso directo a WhatsApp y llamadas para coordinar entregas o dudas.',
        },
        {
          title: 'Preferencias de Entrega',
          desc: 'Direcciones recurrentes y notas especiales de entrega guardadas.',
        },
      ],
      eta: 'Sprint 4',
    },
    settings: {
      key: 'settings',
      title: 'Configuración Avanzada',
      subtitle: 'Pasarelas de pago, notificaciones e integraciones de la cuenta.',
      description:
        'Personaliza los métodos de cobro digital (Mercado Pago, Yape/Plin), reglas de mensajería automática y opciones de facturación para tu negocio.',
      sprint: 'Sprint 5 · Integraciones & Pagos',
      sprintBadgeClass:
        'bg-surface-container-high text-on-surface-variant border-outline-variant/40',
      iconName: 'matTuneOutline',
      iconBgClass: 'bg-surface-container-high border-outline-variant/30',
      iconTextClass: 'text-on-surface',
      features: [
        {
          title: 'Pasarela Mercado Pago',
          desc: 'Conecta tu cuenta de cobros para recibir pagos con tarjeta y billeteras digitales.',
        },
        {
          title: 'Plantillas de Mensajes',
          desc: 'Configura respuestas rápidas y confirmaciones automáticas de pedidos.',
        },
        {
          title: 'Políticas y Términos',
          desc: 'Personaliza tus políticas de cancelación, cambios y tiempos de preparación.',
        },
      ],
      eta: 'Sprint 5',
    },
  };

  DEFAULT_CONFIG: ModuleConfig = {
    key: 'default',
    title: 'Módulo en Desarrollo',
    subtitle: 'Esta funcionalidad está siendo construida activamente por el equipo.',
    description:
      'Estamos trabajando en la implementación de esta sección siguiendo nuestro roadmap de desarrollo por sprints para ofrecerte la mejor experiencia operativa.',
    sprint: 'Roadmap JaldiShop',
    sprintBadgeClass: 'bg-surface-container-high text-on-surface-variant border-outline-variant/40',
    iconName: 'matHourglassTopOutline',
    iconBgClass: 'bg-surface-container-high border-outline-variant/30',
    iconTextClass: 'text-primary-container',
    features: [
      {
        title: 'En desarrollo continuo',
        desc: 'El equipo de ingeniería avanza según el backlog planificado en Scrum.',
      },
      {
        title: 'Calidad asegurada',
        desc: 'Cada módulo se integra con pruebas unitarias y cobertura completa antes de su apertura.',
      },
    ],
    eta: 'Próximamente',
  };
}
