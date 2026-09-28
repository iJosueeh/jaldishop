import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminQuickStores } from './admin-quick-stores';
import { provideRouter } from '@angular/router';

describe('AdminQuickStores', () => {
  let component: AdminQuickStores;
  let fixture: ComponentFixture<AdminQuickStores>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminQuickStores],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminQuickStores);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('stores', [
      {
        id: 'str-1',
        name: 'Dulce Capri',
        slug: 'dulce-capri',
        status: 'ACTIVE',
      },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar la lista de tiendas', () => {
    expect(component).toBeTruthy();
    expect(component.stores().length).toBe(1);
  });
});
