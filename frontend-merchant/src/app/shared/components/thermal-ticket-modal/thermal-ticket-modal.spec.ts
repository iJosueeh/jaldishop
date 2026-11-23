import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ThermalTicketModal } from './thermal-ticket-modal';
import { ToastService } from '../../../core/services/toast.service';

describe('ThermalTicketModal', () => {
  let component: ThermalTicketModal;
  let fixture: ComponentFixture<ThermalTicketModal>;
  let toastService: ToastService;

  const mockOrder = {
    orderNumber: '#ORD-00000099',
    customerName: 'Mario Vargas',
    customerPhone: '912345678',
    deliveryMode: 'PICKUP',
    totalAmount: 45.0,
    items: [
      {
        name: 'Empanada Gallega',
        quantity: 3,
        unitPrice: 15.0,
        totalPrice: 45.0,
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThermalTicketModal],
      providers: [provideHttpClient(), provideHttpClientTesting(), ToastService],
    }).compileComponents();

    fixture = TestBed.createComponent(ThermalTicketModal);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe generar contenido ASCII a partir de orden individual', () => {
    fixture.componentRef.setInput('order', mockOrder);
    fixture.componentRef.setInput('storeName', 'Mi Panadería');
    fixture.detectChanges();

    const ascii = component.asciiContent();
    expect(ascii).toContain('MI PANADERÍA');
    expect(ascii).toContain('#ORD-00000099');
    expect(ascii).toContain('Mario Vargas');
    expect(ascii).toContain('Empanada Gallega');
    expect(ascii).toContain('S/ 45.00');
  });

  it('debe cambiar ancho de papel entre 80mm y 58mm', () => {
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();

    component.setPaperWidth(58);
    expect(component.paperWidth()).toBe(58);
    expect(component.asciiContent()).toContain('#ORD-00000099');

    component.setPaperWidth(80);
    expect(component.paperWidth()).toBe(80);
  });

  it('debe generar contenido batch cuando se suministra lista de órdenes', () => {
    fixture.componentRef.setInput('orders', [mockOrder, mockOrder]);
    fixture.detectChanges();

    const ascii = component.asciiContent();
    expect(ascii).toContain('CORTE DE COMANDA');
  });

  it('debe emitir closeModal al invocar onClose', () => {
    const closeSpy = vi.fn();
    component.closeModal.subscribe(closeSpy);
    component.onClose();
    expect(closeSpy).toHaveBeenCalled();
  });

  it('debe invocar fallbackCopy o clipboard al copiar texto', async () => {
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();

    const toastSpy = vi.spyOn(toastService, 'success');
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });

    component.copyToClipboard();
    await Promise.resolve();

    expect(toastSpy).toHaveBeenCalledWith('Comanda ASCII copiada al portapapeles.');
  });
});
