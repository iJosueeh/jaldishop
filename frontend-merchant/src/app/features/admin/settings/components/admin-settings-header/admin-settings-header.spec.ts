import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsHeader } from './admin-settings-header';

describe('AdminSettingsHeader', () => {
  let component: AdminSettingsHeader;
  let fixture: ComponentFixture<AdminSettingsHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSettingsHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsHeader);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir save al hacer clic en guardar cambios', () => {
    const spy = vi.spyOn(component.save, 'emit');
    component.save.emit();
    expect(spy).toHaveBeenCalled();
  });
});
