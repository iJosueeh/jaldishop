import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastContainer } from './toast-container';
import { ToastService } from '../../../core/services/toast.service';

describe('ToastContainer', () => {
  let component: ToastContainer;
  let fixture: ComponentFixture<ToastContainer>;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastContainer],
    }).compileComponents();

    toastService = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(ToastContainer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe renderizar toasts cuando existan en ToastService', () => {
    toastService.success('Operación completada', 'Éxito');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Éxito');
    expect(compiled.textContent).toContain('Operación completada');
  });

  it('debe permitir descartar un toast al presionar cerrar', () => {
    const id = toastService.error('Error de prueba');
    fixture.detectChanges();

    component.dismiss(id);
    fixture.detectChanges();

    expect(toastService.toasts().length).toBe(0);
  });
});
