import { inject, Injectable, signal } from '@angular/core';
import {
  GeneralPreferences,
  MerchantSettings,
  NotificationPreferences,
  OrderPreferences,
  WhatsAppTemplate,
  WhatsAppTemplateId,
} from '../models/settings.models';
import { ToastService } from './toast.service';

const SETTINGS_STORAGE_KEY = 'jaldishop_merchant_settings_v1';

const DEFAULT_TEMPLATES: WhatsAppTemplate[] = [
  {
    id: 'in_preparation',
    title: 'Pedido en Preparación',
    description: 'Se envía cuando comienzas a cocinar o armar el pedido del cliente.',
    message: '¡Hola {cliente}! 👋 Tu pedido *#{numero_pedido}* ya está en preparación en {tienda}. Te avisaremos en cuanto esté listo. 👩‍🍳✨',
    availableTags: ['{cliente}', '{numero_pedido}', '{tienda}', '{total}', '{modalidad}'],
  },
  {
    id: 'ready',
    title: 'Pedido Listo para Entrega / Recojo',
    description: 'Se envía cuando el empaque está finalizado o el repartidor va en camino.',
    message: '¡Buenas noticias, {cliente}! 🎉 Tu pedido *#{numero_pedido}* ({modalidad}) está listo. Total: *{total}*. ¡Gracias por tu preferencia!',
    availableTags: ['{cliente}', '{numero_pedido}', '{tienda}', '{total}', '{modalidad}'],
  },
  {
    id: 'completed',
    title: 'Pedido Completado / Agradecimiento',
    description: 'Se envía al entregar el pedido para fidelizar y solicitar opinión.',
    message: '¡Muchas gracias por tu compra en {tienda}, {cliente}! 🌟 Esperamos que disfrutes tu pedido *#{numero_pedido}*. ¡Buen provecho!',
    availableTags: ['{cliente}', '{numero_pedido}', '{tienda}'],
  },
];

const DEFAULT_SETTINGS: MerchantSettings = {
  whatsappTemplates: DEFAULT_TEMPLATES,
  notifications: {
    orderSoundAlert: true,
    lowStockVisualAlert: true,
    dailyEmailSummary: false,
    soundVolume: 80,
  },
  orderPreferences: {
    minDeliveryAmount: null,
    postCheckoutMessage: '¡Gracias por tu compra! Procesaremos tu orden con la mayor dedicación y te notificaremos cuando esté lista.',
    estimatedPreparationTimeMinutes: 30,
  },
  paymentMethods: {
    mercadoPago: {
      enabled: true,
      sandboxMode: true,
      publicKey: '',
    },
    yapePlin: {
      enabled: true,
      phoneNumber: '987654321',
      accountHolder: 'Mi Tienda',
      instructions: 'Envía tu comprobante o captura de pantalla al WhatsApp de la tienda indicando tu número de pedido.',
    },
    bankTransfer: {
      enabled: false,
      bankName: 'BCP',
      accountNumber: '191-12345678-0-12',
      cci: '00219100123456780012',
      accountHolder: 'Mi Tienda S.A.C.',
    },
    cashOnDelivery: {
      enabled: true,
      acceptsCardsOnDelivery: false,
    },
  },
  policies: {
    cancellationPolicy: 'Puedes cancelar tu pedido de forma gratuita mientras se encuentre en estado "Confirmado". Una vez iniciada la preparación en cocina/taller, no se admiten cancelaciones.',
    refundPolicy: 'En caso de producto defectuoso o inconveniente comprobado en la entrega, se realizará el reembolso total o reenvío del producto en un plazo máximo de 24 horas.',
    requireCustomerDni: false,
    allowOrderNotes: true,
  },
  general: {
    currency: 'PEN',
    currencySymbol: 'S/',
    timezone: 'America/Lima (UTC-5)',
    locale: 'es-PE',
  },
};

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private readonly toastService = inject(ToastService);
  readonly settings = signal<MerchantSettings>(this.loadSettings());

  private loadSettings(): MerchantSettings {
    try {
      const stored = localStorage.getItem(SETTINGS_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        return {
          ...DEFAULT_SETTINGS,
          ...parsed,
          whatsappTemplates: parsed.whatsappTemplates || DEFAULT_TEMPLATES,
          notifications: { ...DEFAULT_SETTINGS.notifications, ...parsed.notifications },
          orderPreferences: { ...DEFAULT_SETTINGS.orderPreferences, ...parsed.orderPreferences },
          paymentMethods: { ...DEFAULT_SETTINGS.paymentMethods, ...parsed.paymentMethods },
          policies: { ...DEFAULT_SETTINGS.policies, ...parsed.policies },
          general: { ...DEFAULT_SETTINGS.general, ...parsed.general },
        };
      }
    } catch {
      // Si falla lectura de localStorage, fallback a defaults
    }
    return DEFAULT_SETTINGS;
  }

  saveSettings(newSettings: MerchantSettings): void {
    try {
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(newSettings));
      this.settings.set(newSettings);
      this.toastService.success('Configuración guardada correctamente.');
    } catch {
      this.toastService.error('No se pudo guardar la configuración.');
    }
  }

  updateWhatsAppTemplate(id: WhatsAppTemplateId, message: string): void {
    const current = this.settings();
    const updated = current.whatsappTemplates.map((tpl) =>
      tpl.id === id ? { ...tpl, message } : tpl
    );
    this.saveSettings({ ...current, whatsappTemplates: updated });
  }

  updateNotifications(prefs: Partial<NotificationPreferences>): void {
    const current = this.settings();
    const updated: NotificationPreferences = { ...current.notifications, ...prefs };
    this.saveSettings({ ...current, notifications: updated });
  }

  updateOrderPreferences(prefs: Partial<OrderPreferences>): void {
    const current = this.settings();
    const updated: OrderPreferences = { ...current.orderPreferences, ...prefs };
    this.saveSettings({ ...current, orderPreferences: updated });
  }

  updatePaymentMethods(methods: Partial<MerchantSettings['paymentMethods']>): void {
    const current = this.settings();
    const updated = { ...current.paymentMethods, ...methods };
    this.saveSettings({ ...current, paymentMethods: updated });
  }

  updatePolicies(policies: Partial<MerchantSettings['policies']>): void {
    const current = this.settings();
    const updated = { ...current.policies, ...policies };
    this.saveSettings({ ...current, policies: updated });
  }

  resetWhatsAppTemplates(): void {
    const current = this.settings();
    this.saveSettings({ ...current, whatsappTemplates: DEFAULT_TEMPLATES });
    this.toastService.info('Plantillas de WhatsApp restauradas a valores predeterminados.');
  }

  /**
   * Genera un tono de notificación armónico usando Web Audio API sin requerir MP3 externo.
   */
  playNotificationSound(): void {
    if (!this.settings().notifications.orderSoundAlert) {
      return;
    }

    try {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;

      const ctx = new AudioCtx();
      const gain = ctx.createGain();
      const volume = (this.settings().notifications.soundVolume / 100) * 0.3;
      gain.gain.setValueAtTime(volume, ctx.currentTime);
      gain.connect(ctx.destination);

      // Primer tono (campana alta - Mi6 ~ 1318 Hz)
      const osc1 = ctx.createOscillator();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(1318.51, ctx.currentTime);
      osc1.connect(gain);
      osc1.start(ctx.currentTime);
      osc1.stop(ctx.currentTime + 0.18);

      // Segundo tono (Sol#6 ~ 1661 Hz)
      const osc2 = ctx.createOscillator();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(1661.22, ctx.currentTime + 0.12);
      osc2.connect(gain);
      osc2.start(ctx.currentTime + 0.12);
      osc2.stop(ctx.currentTime + 0.45);

      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.5);
    } catch {
      // Ignorar si el navegador bloquea audio autoplay
    }
  }
}
