import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { Dashboards } from './dashboards';
import { CapacityService } from '../../core/services/capacity.service';

describe('Dashboards', () => {
  let component: Dashboards;
  let fixture: ComponentFixture<Dashboards>;

  beforeEach(async () => {
    const mockCapacityService = {
      configurations: signal([]),
      todayConfigurations: signal([]),
      todayTotalCapacity: signal(0),
      exceptions: signal([]),
      todayException: signal(null),
      todayEffectiveCapacity: signal(0),
      isTodayClosed: signal(false),
      getConfigurations: () => of([]),
      getExceptions: () => of([]),
    };

    await TestBed.configureTestingModule({
      imports: [Dashboards],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CapacityService, useValue: mockCapacityService },
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

