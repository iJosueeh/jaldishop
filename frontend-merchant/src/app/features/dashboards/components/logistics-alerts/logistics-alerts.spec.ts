import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LogisticsAlerts } from './logistics-alerts';

describe('LogisticsAlerts', () => {
  let component: LogisticsAlerts;
  let fixture: ComponentFixture<LogisticsAlerts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogisticsAlerts],
      providers: [
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogisticsAlerts);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
