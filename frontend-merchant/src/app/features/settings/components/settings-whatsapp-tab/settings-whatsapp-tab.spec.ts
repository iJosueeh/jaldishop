import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SettingsWhatsappTab } from './settings-whatsapp-tab';

describe('SettingsWhatsappTab', () => {
  let component: SettingsWhatsappTab;
  let fixture: ComponentFixture<SettingsWhatsappTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsWhatsappTab],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsWhatsappTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch templates and insert tags', () => {
    component.selectTemplate('ready');
    expect(component.selectedTemplateId()).toBe('ready');

    component.insertTag('{total}');
    expect(component.editedMessage()).toContain('{total}');
  });
});

