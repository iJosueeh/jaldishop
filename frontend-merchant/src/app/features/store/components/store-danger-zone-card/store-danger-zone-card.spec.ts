import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreDangerZoneCard } from './store-danger-zone-card';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('StoreDangerZoneCard', () => {
  let component: StoreDangerZoneCard;
  let fixture: ComponentFixture<StoreDangerZoneCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreDangerZoneCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreDangerZoneCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe traducir correctamente el estado ACTIVE a español', () => {
    fixture.componentRef.setInput('status', 'ACTIVE');
    fixture.detectChanges();

    const info = component.statusInfo();
    expect(info.label).toBe('Activa y Operativa');
    expect(info.description).toContain('publicado y abierto');
    expect(info.badgeClass).toContain('emerald');
  });

  it('debe traducir correctamente el estado INACTIVE a español', () => {
    fixture.componentRef.setInput('status', 'INACTIVE');
    fixture.detectChanges();

    const info = component.statusInfo();
    expect(info.label).toBe('Inactiva / Pausada');
    expect(info.description).toContain('oculta al público');
    expect(info.badgeClass).toContain('amber');
  });

  it('debe emitir requestClose cuando el comerciante hace clic y no está procesando', () => {
    const spy = vi.fn();
    component.requestClose.subscribe(spy);

    component.onRequestClose();
    expect(spy).toHaveBeenCalled();
  });

  it('no debe emitir requestClose si isClosing es verdadero', () => {
    fixture.componentRef.setInput('isClosing', true);
    fixture.detectChanges();

    const spy = vi.fn();
    component.requestClose.subscribe(spy);

    component.onRequestClose();
    expect(spy).not.toHaveBeenCalled();
  });
});
