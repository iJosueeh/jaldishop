import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('debe crearse con la lista de toasts vacía', () => {
    expect(service).toBeTruthy();
    expect(service.toasts().length).toBe(0);
  });

  it('debe agregar toast de éxito correctamente', () => {
    const id = service.success('Tienda guardada con éxito');
    const toasts = service.toasts();

    expect(toasts.length).toBe(1);
    expect(toasts[0].id).toBe(id);
    expect(toasts[0].type).toBe('success');
    expect(toasts[0].message).toBe('Tienda guardada con éxito');
  });

  it('debe agregar toast de error correctamente', () => {
    service.error('Error al procesar pedido');
    const toasts = service.toasts();

    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('error');
    expect(toasts[0].message).toBe('Error al procesar pedido');
  });

  it('debe agregar toast de advertencia e información', () => {
    service.warning('Capacidad casi al límite');
    service.info('Nuevo horario disponible');

    const toasts = service.toasts();
    expect(toasts.length).toBe(2);
    expect(toasts[0].type).toBe('warning');
    expect(toasts[1].type).toBe('info');
  });

  it('debe descartar un toast por su ID', () => {
    const id1 = service.success('Mensaje 1');
    const id2 = service.error('Mensaje 2');

    expect(service.toasts().length).toBe(2);

    service.dismiss(id1);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].id).toBe(id2);
  });

  it('debe limpiar todos los toasts al invocar clear()', () => {
    service.success('1');
    service.error('2');
    service.warning('3');

    expect(service.toasts().length).toBe(3);

    service.clear();
    expect(service.toasts().length).toBe(0);
  });
});
