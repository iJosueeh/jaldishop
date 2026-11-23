import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DonutDistributionChartComponent } from './donut-distribution-chart';

describe('DonutDistributionChartComponent', () => {
  let component: DonutDistributionChartComponent;
  let fixture: ComponentFixture<DonutDistributionChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DonutDistributionChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DonutDistributionChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe computar series, total y etiquetas correctamente', () => {
    const mockSegments = [
      { label: 'Comercios', value: 8500, color: '#4f46e5' },
      { label: 'JaldiShop', value: 500, color: '#10b981' },
      { label: 'Envíos', value: 1000, color: '#f59e0b' },
    ];

    fixture.componentRef.setInput('segments', mockSegments);
    fixture.detectChanges();

    expect(component.labels()).toEqual(['Comercios', 'JaldiShop', 'Envíos']);
    expect(component.series()).toEqual([8500, 500, 1000]);
    expect(component.totalValue()).toBe(10000);
  });
});
