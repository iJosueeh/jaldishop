import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ComingSoon } from './coming-soon';

describe('ComingSoon', () => {
  let component: ComingSoon;
  let fixture: ComponentFixture<ComingSoon>;

  describe('con configuración por defecto', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ComingSoon],
        providers: [
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: {},
                url: [],
              },
            },
          },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ComingSoon);
      component = fixture.componentInstance;
      await fixture.whenStable();
    });

    it('debe crearse correctamente', () => {
      expect(component).toBeTruthy();
    });

    it('debe contener la configuración por defecto', () => {
      const config = component.config();
      expect(config).toBeDefined();
      expect(config.title).toBe('Módulo en Desarrollo');
      expect(config.features.length).toBeGreaterThan(0);
    });
  });

  describe('con módulo products', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ComingSoon],
        providers: [
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: { moduleKey: 'products' },
                url: [{ path: 'products' }],
              },
            },
          },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ComingSoon);
      component = fixture.componentInstance;
      await fixture.whenStable();
    });

    it('debe cargar la configuración del módulo Catálogo y Productos', () => {
      const config = component.config();
      expect(config.key).toBe('products');
      expect(config.title).toBe('Catálogo y Productos');
      expect(config.sprint).toContain('Sprint 3');
      expect(config.features.length).toBe(3);
    });

    it('debe renderizar el título en el template', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('h1')?.textContent).toContain('Catálogo y Productos');
    });
  });

  describe('con módulo orders', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ComingSoon],
        providers: [
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: { moduleKey: 'orders' },
                url: [{ path: 'orders' }],
              },
            },
          },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ComingSoon);
      component = fixture.componentInstance;
      await fixture.whenStable();
    });

    it('debe cargar la configuración del módulo Pedidos', () => {
      const config = component.config();
      expect(config.key).toBe('orders');
      expect(config.title).toBe('Gestión de Pedidos en Vivo');
      expect(config.sprint).toContain('Sprint 4');
    });
  });
});

