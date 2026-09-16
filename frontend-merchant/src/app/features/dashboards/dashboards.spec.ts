import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { Dashboards } from './dashboards';

describe('Dashboards', () => {
  let component: Dashboards;
  let fixture: ComponentFixture<Dashboards>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboards],
      providers: [
        provideRouter([]),
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboards);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
