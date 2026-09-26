import { TestBed } from '@angular/core/testing';
import { SettingsService } from './settings.service';
import { ToastService } from './toast.service';
import { vi } from 'vitest';

describe('SettingsService', () => {
  let service: SettingsService;
  let toastMock: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    toastMock = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        SettingsService,
        { provide: ToastService, useValue: toastMock },
      ],
    });

    service = TestBed.inject(SettingsService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created with default settings', () => {
    expect(service).toBeTruthy();
    const settings = service.settings();
    expect(settings.whatsappTemplates.length).toBe(3);
    expect(settings.notifications.orderSoundAlert).toBe(true);
    expect(settings.general.currency).toBe('PEN');
  });

  it('should update whatsapp template message and save', () => {
    service.updateWhatsAppTemplate('in_preparation', 'Mensaje actualizado');
    const updated = service.settings().whatsappTemplates.find((t) => t.id === 'in_preparation');
    expect(updated?.message).toBe('Mensaje actualizado');
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('should update notifications preferences', () => {
    service.updateNotifications({ orderSoundAlert: false, soundVolume: 50 });
    const notifs = service.settings().notifications;
    expect(notifs.orderSoundAlert).toBe(false);
    expect(notifs.soundVolume).toBe(50);
  });

  it('should update order preferences', () => {
    service.updateOrderPreferences({ minDeliveryAmount: 25.0 });
    expect(service.settings().orderPreferences.minDeliveryAmount).toBe(25.0);
  });

  it('should update payment methods', () => {
    service.updatePaymentMethods({ yapePlin: { enabled: true, phoneNumber: '999888777', accountHolder: 'Nuevo Titular', instructions: 'Instrucciones' } });
    expect(service.settings().paymentMethods.yapePlin.phoneNumber).toBe('999888777');
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('should update policies', () => {
    service.updatePolicies({ requireCustomerDni: true });
    expect(service.settings().policies.requireCustomerDni).toBe(true);
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('should reset whatsapp templates to default', () => {
    service.updateWhatsAppTemplate('ready', 'Custom ready');
    service.resetWhatsAppTemplates();
    const ready = service.settings().whatsappTemplates.find((t) => t.id === 'ready');
    expect(ready?.message).toContain('¡Buenas noticias');
    expect(toastMock.info).toHaveBeenCalled();
  });
});

