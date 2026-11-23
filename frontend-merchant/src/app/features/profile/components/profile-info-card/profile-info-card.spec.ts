import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileInfoCard } from './profile-info-card';
import { FormControl, FormGroup } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { SettingsService } from '../../../../core/services/settings.service';

describe('ProfileInfoCard', () => {
  let component: ProfileInfoCard;
  let fixture: ComponentFixture<ProfileInfoCard>;
  let settingsService: SettingsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileInfoCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileInfoCard);
    component = fixture.componentInstance;
    settingsService = TestBed.inject(SettingsService);
    fixture.componentRef.setInput(
      'form',
      new FormGroup({
        firstName: new FormControl(''),
        lastName: new FormControl(''),
        phone: new FormControl(''),
      })
    );
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir el evento save al llamar a onSubmit', () => {
    let emitted = false;
    component.save.subscribe(() => {
      emitted = true;
    });

    component.onSubmit();
    expect(emitted).toBe(true);
  });

  it('debe actualizar ordenSoundAlert en SettingsService al alternar alerta de pedidos', () => {
    const spy = vi.spyOn(settingsService, 'saveSettings');
    component.onToggleOrderSoundAlert(false);
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ orderSoundAlert: false }));

    component.onToggleOrderSoundAlert(true);
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ orderSoundAlert: true }));
  });

  it('debe actualizar lowStockVisualAlert en SettingsService al alternar alerta de cupo', () => {
    const spy = vi.spyOn(settingsService, 'saveSettings');
    component.onToggleCapacityAlert(false);
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ lowStockVisualAlert: false }));

    component.onToggleCapacityAlert(true);
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ lowStockVisualAlert: true }));
  });
});
