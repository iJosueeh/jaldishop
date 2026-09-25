import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SettingsGeneralTab } from './settings-general-tab';

describe('SettingsGeneralTab', () => {
  let component: SettingsGeneralTab;
  let fixture: ComponentFixture<SettingsGeneralTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsGeneralTab],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsGeneralTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

