import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsPoliciesTab } from './settings-policies-tab';

describe('SettingsPoliciesTab', () => {
  let component: SettingsPoliciesTab;
  let fixture: ComponentFixture<SettingsPoliciesTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsPoliciesTab],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsPoliciesTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
