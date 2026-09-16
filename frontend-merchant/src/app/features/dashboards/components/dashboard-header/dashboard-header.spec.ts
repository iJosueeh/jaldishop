import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { DashboardHeader } from './dashboard-header';

describe('DashboardHeader', () => {
  let component: DashboardHeader;
  let fixture: ComponentFixture<DashboardHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardHeader],
      providers: [
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
