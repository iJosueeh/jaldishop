import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreStatusModal } from './store-status-modal';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';

describe('StoreStatusModal', () => {
  let component: StoreStatusModal;
  let fixture: ComponentFixture<StoreStatusModal>;

  const mockStore: AdminStoreSummary = {
    id: 'str-1',
    merchantUserId: 'usr-1',
    name: 'Dulce Capri',
    slug: 'dulce-capri',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    createdAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-01T10:00:00Z',
    merchant: {
      id: 'usr-1',
      email: 'm@s.com',
      fullName: 'Valeria Ramos',
      status: 'ACTIVE',
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreStatusModal],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreStatusModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('store', mockStore);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y mostrar el nombre de la tienda', () => {
    expect(component).toBeTruthy();
    expect(component.store()?.name).toBe('Dulce Capri');
  });

  it('debe emitir confirm al presionar el botón de confirmación', () => {
    const spy = vi.spyOn(component.confirm, 'emit');
    component.confirm.emit(mockStore);
    expect(spy).toHaveBeenCalledWith(mockStore);
  });

  it('debe emitir cancel al cancelar', () => {
    const spy = vi.spyOn(component.cancel, 'emit');
    component.cancel.emit();
    expect(spy).toHaveBeenCalled();
  });
});
