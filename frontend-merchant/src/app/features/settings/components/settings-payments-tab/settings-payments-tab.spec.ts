import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsPaymentsTab } from './settings-payments-tab';

describe('SettingsPaymentsTab', () => {
  let component: SettingsPaymentsTab;
  let fixture: ComponentFixture<SettingsPaymentsTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsPaymentsTab],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsPaymentsTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
