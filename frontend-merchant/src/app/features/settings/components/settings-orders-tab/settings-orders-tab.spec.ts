import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsOrdersTab } from './settings-orders-tab';

describe('SettingsOrdersTab', () => {
  let component: SettingsOrdersTab;
  let fixture: ComponentFixture<SettingsOrdersTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsOrdersTab],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsOrdersTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
