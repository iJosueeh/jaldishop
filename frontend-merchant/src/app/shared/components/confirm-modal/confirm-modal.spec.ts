import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmModal } from './confirm-modal';

describe('ConfirmModal', () => {
  let component: ConfirmModal;
  let fixture: ComponentFixture<ConfirmModal>;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [ConfirmModal],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('title', '¿Eliminar imagen?');
    fixture.componentRef.setInput('message', '¿Estás seguro de que deseas eliminar esta imagen?');
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('debe crearse y renderizar cuando isOpen es true', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(true);
    expect(component.isRendered()).toBe(true);
    expect(component.isClosing()).toBe(false);
  });

  it('debe emitir confirm y confirmed tras completar la animación de salida', () => {
    let confirmEmitted = false;
    let confirmedEmitted = false;
    component.confirm.subscribe(() => {
      confirmEmitted = true;
    });
    component.confirmed.subscribe(() => {
      confirmedEmitted = true;
    });

    component.onConfirm();
    expect(component.isClosing()).toBe(true);
    expect(confirmEmitted).toBe(false);

    vi.advanceTimersByTime(250);
    expect(confirmEmitted).toBe(true);
    expect(confirmedEmitted).toBe(true);
    expect(component.isRendered()).toBe(false);
    expect(component.isClosing()).toBe(false);
  });

  it('debe emitir cancel y cancelled tras completar la animación de salida', () => {
    let cancelEmitted = false;
    let cancelledEmitted = false;
    component.cancel.subscribe(() => {
      cancelEmitted = true;
    });
    component.cancelled.subscribe(() => {
      cancelledEmitted = true;
    });

    component.onCancel();
    expect(component.isClosing()).toBe(true);
    expect(cancelEmitted).toBe(false);

    vi.advanceTimersByTime(250);
    expect(cancelEmitted).toBe(true);
    expect(cancelledEmitted).toBe(true);
    expect(component.isRendered()).toBe(false);
    expect(component.isClosing()).toBe(false);
  });

  it('debe llamar onCancel al hacer clic en el backdrop', () => {
    let cancelled = false;
    component.cancelled.subscribe(() => {
      cancelled = true;
    });

    const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
    backdrop?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(250);
    expect(cancelled).toBe(true);
  });

  it('debe llamar onCancel al presionar la tecla Escape', () => {
    let cancelled = false;
    component.cancelled.subscribe(() => {
      cancelled = true;
    });

    component.onEscape();
    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(250);
    expect(cancelled).toBe(true);
  });
});
