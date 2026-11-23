import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { DashboardHeader } from './dashboard-header';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { environment } from '../../../../../environments/environment';
import { StoreResponse } from '../../../../core/models/store.models';

describe('DashboardHeader', () => {
  let component: DashboardHeader;
  let fixture: ComponentFixture<DashboardHeader>;
  let storeService: StoreService;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardHeader],
      providers: [
        provideHttpClient(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardHeader);
    component = fixture.componentInstance;
    storeService = TestBed.inject(StoreService);
    toastService = TestBed.inject(ToastService);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe copiar el enlace canonico del catalogo publico usando marketplaceUrl y slug de la tienda', async () => {
    const mockStore: Partial<StoreResponse> = {
      id: 'store-123',
      name: 'Mi Panadería',
      slug: 'mi-panaderia',
      status: 'ACTIVE',
    };
    storeService.currentStore.set(mockStore as StoreResponse);

    const writeTextSpy = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: { writeText: writeTextSpy },
    });
    const toastSpy = vi.spyOn(toastService, 'success');

    component.copyStoreLink();

    expect(writeTextSpy).toHaveBeenCalledWith(`${environment.marketplaceUrl}/tienda/mi-panaderia`);
    await Promise.resolve();
    expect(toastSpy).toHaveBeenCalledWith('¡Enlace del catálogo copiado al portapapeles!');
  });
});
