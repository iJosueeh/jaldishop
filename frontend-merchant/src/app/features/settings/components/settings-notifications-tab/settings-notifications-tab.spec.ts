import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsNotificationsTab } from './settings-notifications-tab';

describe('SettingsNotificationsTab', () => {
  let component: SettingsNotificationsTab;
  let fixture: ComponentFixture<SettingsNotificationsTab>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsNotificationsTab],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsNotificationsTab);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
